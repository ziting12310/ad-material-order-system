package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.FeedbackStatus;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
import com.peak.dto.ArchiveOrderDTO;
import com.peak.dto.FinalAuditDTO;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.entity.OrderFeedback;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderFeedbackMapper;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.UserMapper;
import com.peak.service.FinalAuditService;
import com.peak.vo.ArchiveOrderResponseVO;
import com.peak.vo.FinalAuditResponseVO;
import com.peak.vo.OrderAuditInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 定稿审核与交付归档服务实现 — C 模块 M5 + M6
 *
 * <p>状态流转说明：
 *  - confirmFinal: WAIT_APPROVE → APPROVED（定稿）
 *  - rejectFinal:  WAIT_APPROVE → DESIGNING（驳回，回退到设计中）
 *  - archiveOrder: APPROVED → DELIVERED + archived=1（交付归档，归档后只读）
 *
 * <p>所有状态变更均通过 updateById 触发 MyBatis-Plus 乐观锁（@Version 注解），
 * 版本不匹配时更新行数为 0 → 抛出 ORDER_VERSION_CONFLICT (409)。
 *
 * <p>模块边界遵守：
 *  - 不修改 A 模块（OrderServiceImpl 等）代码
 *  - 不修改 B 模块（认证、权限基础设施）代码
 *  - 不修改 D 模块（设计版本、反馈意见）代码
 *  - 不修改枚举类
 *  - 复用已有 Mapper、实体、错误码、枚举
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinalAuditServiceImpl implements FinalAuditService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final DesignVersionMapper designVersionMapper;
    private final OrderFeedbackMapper orderFeedbackMapper;

    // ========================================================================
    // 【1】confirmFinal — 确认定稿（WAIT_APPROVE → APPROVED）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinalAuditResponseVO confirmFinal(Long orderId, FinalAuditDTO dto, Long operatorId) {
        // ① 查订单 + 存在性校验
        Order order = findOrderOrThrow(orderId);

        // ② 归档只读校验：已归档订单不允许任何修改操作
        checkNotArchived(order, "确认定稿");

        // ③ 状态校验：仅 WAIT_APPROVE 可确认定稿
        checkStatus(order, OrderStatus.WAIT_APPROVE, "确认定稿");

        // ④ 权限校验：业务负责人(BL) 或 客户经理(AM) 可操作
        requireAnyRole(operatorId, UserRole.BL.getCode(), UserRole.AM.getCode());

        // ⑤ 前置校验：必须有设计版本才能定稿
        DesignVersion latest = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getOrderId, orderId)
                        .orderByDesc(DesignVersion::getId)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND, "尚未上传设计稿，无法定稿");
        }

        // ⑥ 乐观锁版本校验
        checkVersion(order, dto.getVersion());

        // ⑦ 标记最新版本为 final
        latest.setFinalVersion(1);
        designVersionMapper.updateById(latest);

        // ⑧ 将所有 PENDING 状态的 Feedback 置为 PROCESSED（定稿后意见全部处理完毕）
        LocalDateTime now = LocalDateTime.now();
        orderFeedbackMapper.update(null,
                new LambdaUpdateWrapper<OrderFeedback>()
                        .eq(OrderFeedback::getOrderId, orderId)
                        .eq(OrderFeedback::getStatus, FeedbackStatus.PENDING.getCode())
                        .set(OrderFeedback::getStatus, FeedbackStatus.PROCESSED.getCode())
                        .set(OrderFeedback::getProcessedAt, now)
        );

        // ⑨ 更新订单：状态 → APPROVED + 定稿人 + 定稿时间 + 定稿版本ID
        order.setStatus(OrderStatus.APPROVED.getCode());
        order.setApprovedBy(operatorId);
        order.setApprovedAt(now);
        order.setFinalVersionId(latest.getId());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        // ⑩ 组装响应
        User operator = userMapper.selectById(operatorId);
        FinalAuditResponseVO resp = new FinalAuditResponseVO();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setVersion(order.getVersion());
        resp.setApprovedBy(operatorId);
        resp.setApprovedByName(operator != null ? operator.getName() : null);
        resp.setApprovedAt(now);
        resp.setFinalVersionId(latest.getId());
        resp.setFinalVersionNo(latest.getVersionNo());
        return resp;
    }

    // ========================================================================
    // 【2】rejectFinal — 驳回定稿（WAIT_APPROVE → DESIGNING）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinalAuditResponseVO rejectFinal(Long orderId, FinalAuditDTO dto, Long operatorId) {
        // ① 查订单 + 存在性校验
        Order order = findOrderOrThrow(orderId);

        // ② 归档只读校验
        checkNotArchived(order, "驳回定稿");

        // ③ 状态校验：仅 WAIT_APPROVE 可驳回
        checkStatus(order, OrderStatus.WAIT_APPROVE, "驳回定稿");

        // ④ 权限校验：业务负责人(BL) 或 客户经理(AM) 可操作
        requireAnyRole(operatorId, UserRole.BL.getCode(), UserRole.AM.getCode());

        // ⑤ 乐观锁版本校验
        checkVersion(order, dto.getVersion());

        // ⑥ 查询当前最新设计版本（用于关联驳回意见）
        DesignVersion latest = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getOrderId, orderId)
                        .orderByDesc(DesignVersion::getId)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND, "订单下无设计版本，无法驳回");
        }

        // ⑦ 更新订单状态 → DESIGNING（驳回后设计师继续修改）
        order.setStatus(OrderStatus.DESIGNING.getCode());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        // ⑧ 记录驳回意见到 order_feedbacks（复用已有表，与 A 模块 rejectDesign 模式一致）
        LocalDateTime now = LocalDateTime.now();
        OrderFeedback feedback = new OrderFeedback();
        feedback.setOrderId(order.getId());
        feedback.setVersionId(latest.getId());
        feedback.setContent(dto.getRemark() != null
                ? StringEscapeUtils.escapeHtml4(dto.getRemark())
                : "驳回定稿");
        feedback.setStatus(FeedbackStatus.PENDING.getCode());
        feedback.setSubmittedBy(operatorId);
        feedback.setSubmittedAt(now);
        orderFeedbackMapper.insert(feedback);

        // ⑧ 组装响应
        FinalAuditResponseVO resp = new FinalAuditResponseVO();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【3】archiveOrder — 订单交付归档（APPROVED → DELIVERED + archived=1）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveOrderResponseVO archiveOrder(Long orderId, ArchiveOrderDTO dto, Long operatorId) {
        // ① 查订单 + 存在性校验
        Order order = findOrderOrThrow(orderId);

        // ② 已归档拦截：不能重复归档
        if (Integer.valueOf(1).equals(order.getArchived())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单已归档，不可重复操作");
        }

        // ③ 状态校验：仅 APPROVED（已定稿）可归档
        checkStatus(order, OrderStatus.APPROVED, "交付归档");

        // ④ 权限校验：仅业务负责人(BL) 可归档
        requireRole(operatorId, UserRole.BL.getCode());

        // ⑤ 乐观锁版本校验
        checkVersion(order, dto.getVersion());

        // ⑥ 更新订单：状态 → DELIVERED + 归档人 + 归档时间 + 归档备注 + archived=1
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.DELIVERED.getCode());
        order.setDeliveredBy(operatorId);
        order.setDeliveredAt(now);
        order.setDeliverRemark(dto.getRemark() != null
                ? StringEscapeUtils.escapeHtml4(dto.getRemark())
                : null);
        order.setArchived(1);
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        // ⑦ 组装响应
        User operator = userMapper.selectById(operatorId);
        ArchiveOrderResponseVO resp = new ArchiveOrderResponseVO();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setVersion(order.getVersion());
        resp.setIsArchived(true);
        resp.setArchivedBy(operatorId);
        resp.setArchivedByName(operator != null ? operator.getName() : null);
        resp.setArchivedAt(now);
        resp.setArchiveRemark(order.getDeliverRemark());
        return resp;
    }

    // ========================================================================
    // 【4】getAuditInfo — 查询订单定稿归档信息（配套查询）
    // ========================================================================
    @Override
    public OrderAuditInfoVO getAuditInfo(Long orderId) {
        Order order = findOrderOrThrow(orderId);

        OrderAuditInfoVO vo = new OrderAuditInfoVO();

        // 定稿信息
        boolean approved = OrderStatus.APPROVED.getCode().equals(order.getStatus())
                || OrderStatus.DELIVERED.getCode().equals(order.getStatus());
        vo.setIsApproved(approved);
        if (order.getApprovedBy() != null) {
            vo.setApprovedBy(order.getApprovedBy());
            User approver = userMapper.selectById(order.getApprovedBy());
            vo.setApprovedByName(approver != null ? approver.getName() : null);
            vo.setApprovedAt(order.getApprovedAt());
        }
        if (order.getFinalVersionId() != null) {
            vo.setFinalVersionId(order.getFinalVersionId());
            DesignVersion fv = designVersionMapper.selectById(order.getFinalVersionId());
            vo.setFinalVersionNo(fv != null ? fv.getVersionNo() : null);
        }

        // 归档信息
        vo.setIsArchived(Integer.valueOf(1).equals(order.getArchived()));
        if (order.getDeliveredBy() != null) {
            vo.setArchivedBy(order.getDeliveredBy());
            User archiver = userMapper.selectById(order.getDeliveredBy());
            vo.setArchivedByName(archiver != null ? archiver.getName() : null);
            vo.setArchivedAt(order.getDeliveredAt());
            vo.setArchiveRemark(order.getDeliverRemark());
        }

        return vo;
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    /** 查订单，不存在抛 ORDER_NOT_FOUND */
    private Order findOrderOrThrow(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /** 归档只读校验：已归档订单拒绝所有修改操作 */
    private void checkNotArchived(Order order, String actionLabel) {
        if (Integer.valueOf(1).equals(order.getArchived())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "订单已归档，不允许" + actionLabel + "操作");
        }
    }

    /** 状态前置校验：当前状态 ≠ 期望值 → 抛 ORDER_STATUS_CONFLICT */
    private void checkStatus(Order order, OrderStatus expected, String actionLabel) {
        if (!expected.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前订单状态为 [%s]，%s 需要状态为 [%s]",
                    OrderStatus.of(order.getStatus()) != null
                            ? OrderStatus.of(order.getStatus()).getLabel()
                            : order.getStatus(),
                    actionLabel,
                    expected.getLabel());
        }
    }

    /** 乐观锁版本校验 */
    private void checkVersion(Order order, Integer requestVersion) {
        if (!order.getVersion().equals(requestVersion)) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "数据已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    order.getVersion(), requestVersion);
        }
    }

    /** 角色权限校验：当前用户必须是指定角色之一 */
    private void requireAnyRole(Long userId, String... roles) {
        String actualRole = getUserRole(userId);
        for (String role : roles) {
            if (role.equals(actualRole)) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限执行该操作");
    }

    /** 角色权限校验：当前用户必须是指定角色 */
    private void requireRole(Long userId, String requiredRole) {
        String actualRole = getUserRole(userId);
        if (!requiredRole.equals(actualRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "需要 %s 角色才能执行该操作", UserRole.of(requiredRole).getLabel());
        }
    }

    /** 获取用户角色 */
    private String getUserRole(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null ? u.getRole() : null;
    }
}
