package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.FeedbackStatus;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
import com.peak.dto.OrderFeedbackCreateRequest;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.entity.OrderFeedback;
import com.peak.entity.OrderTask;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderFeedbackMapper;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.OrderTaskMapper;
import com.peak.mapper.UserMapper;
import com.peak.service.OrderFeedbackService;
import com.peak.service.OrderTaskService;
import com.peak.vo.OrderFeedbackResponseVO;
import com.peak.vo.OrderFeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 修改意见服务实现 — API.md §5.1 SubmitFeedback
 *
 * <p>伪代码流程：
 *  ① 查订单 → 不存在 → ORDER_NOT_FOUND 404
 *  ② 状态校验（WAIT_APPROVE 才能提意见）→ 冲突 → ORDER_STATUS_CONFLICT 422
 *  ③ 乐观锁校验 → 冲突 → ORDER_VERSION_CONFLICT 409
 *  ④ XSS 清洗意见内容（StringEscapeUtils.escapeHtml4）
 *  ⑤ 查最新版本 ID（作为 version_id 外键关联）
 *  ⑥ 插入 OrderFeedback（PENDING 状态）+ 更新 Order 状态为 DESIGNING（同事务）
 *  ⑦ 组装 OrderFeedbackResponseVO</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFeedbackServiceImpl implements OrderFeedbackService {

    private final OrderMapper orderMapper;
    private final OrderTaskMapper orderTaskMapper;
    private final OrderFeedbackMapper orderFeedbackMapper;
    private final DesignVersionMapper designVersionMapper;
    private final UserMapper userMapper;
    private final OrderTaskService orderTaskService;

    private static final Pattern VERSION_NO_PATTERN = Pattern.compile("V(\\d+)");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderFeedbackResponseVO submitFeedback(OrderFeedbackCreateRequest dto, Long submitterId) {
        // ① 查订单与任务：多物料模型下 taskId 必填且必须属于该订单
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        OrderTask task = orderTaskMapper.selectById(dto.getTaskId());
        if (task == null || !order.getId().equals(task.getOrderId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物料任务不存在或不属于该订单");
        }
        if (Integer.valueOf(1).equals(order.getArchived())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单已归档，不能提交修改意见");
        }

        // ② 角色校验：仅客户经理（AM）/ 业务负责人（BL）可提交修改意见
        User submitter = userMapper.selectById(submitterId);
        if (submitter == null || !(UserRole.AM.getCode().equals(submitter.getRole())
                || UserRole.BL.getCode().equals(submitter.getRole()))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "仅客户经理（AM）或业务负责人（BL）可以提交修改意见");
        }

        if (UserRole.AM.getCode().equals(submitter.getRole())
                && !submitterId.equals(order.getCreatedBy())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "只能给自己的订单提交修改意见");
        }

        // ③ 状态校验：仅任务 WAIT_APPROVE 可提修改意见
        if (!OrderStatus.WAIT_APPROVE.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前任务状态为 [%s]，提交修改意见需要状态为 [待审核]",
                    OrderStatus.of(task.getStatus()) != null
                            ? OrderStatus.of(task.getStatus()).getLabel() : task.getStatus());
        }

        // ④ 乐观锁校验：使用任务版本号
        if (!task.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "任务已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    task.getVersion(), dto.getVersion());
        }

        // ⑤ XSS 清洗意见内容（ARCH §5.1 SEC-001）
        String safeContent = StringEscapeUtils.escapeHtml4(dto.getContent());

        // ⑥ 查该任务最新设计稿版本（按版本号数字取最大）
        DesignVersion latest = findLatestVersion(task.getId());
        if (latest == null) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND,
                    "当前订单还没有设计稿版本，无法提交修改意见");
        }

        // ⑥ 插入 Feedback
        LocalDateTime now = LocalDateTime.now();
        OrderFeedback feedback = new OrderFeedback();
        feedback.setOrderId(order.getId());
        feedback.setTaskId(task.getId());
        feedback.setVersionId(latest.getId());
        feedback.setContent(safeContent);
        feedback.setStatus(FeedbackStatus.PENDING.getCode());
        feedback.setSubmittedBy(submitterId);
        feedback.setSubmittedAt(now);
        orderFeedbackMapper.insert(feedback);

        // ⑦ 交由任务状态机回退任务为 DESIGNING，并聚合订单状态
        orderTaskService.onFeedbackSubmitted(task.getId(), feedback.getId(), submitterId);

        Order refreshedOrder = orderMapper.selectById(order.getId());
        OrderFeedbackResponseVO resp = new OrderFeedbackResponseVO();
        resp.setId(feedback.getId());
        resp.setTaskId(feedback.getTaskId());
        resp.setContent(safeContent);
        resp.setStatus(feedback.getStatus());
        resp.setVersionId(latest.getId());
        resp.setSubmittedAt(now);
        resp.setOrderStatus(refreshedOrder.getStatus());
        resp.setOrderVersion(refreshedOrder.getVersion());
        return resp;
    }

    @Override
    public List<OrderFeedbackVO> listFeedbacks(Long orderId, Long taskId, Long userId, String userRole) {
        Long resolvedOrderId = orderId;
        if (resolvedOrderId == null && taskId != null) {
            OrderFeedback sample = orderFeedbackMapper.selectOne(
                    new LambdaQueryWrapper<OrderFeedback>()
                            .eq(OrderFeedback::getTaskId, taskId)
                            .orderByAsc(OrderFeedback::getId)
                            .last("LIMIT 1"));
            if (sample == null) {
                return List.of();
            }
            resolvedOrderId = sample.getOrderId();
        }
        Order order = requireOrderForRead(resolvedOrderId, taskId, userId, userRole);

        LambdaQueryWrapper<OrderFeedback> wrapper = new LambdaQueryWrapper<OrderFeedback>()
                .eq(OrderFeedback::getOrderId, order.getId());
        if (taskId != null) {
            wrapper.eq(OrderFeedback::getTaskId, taskId);
        }
        List<OrderFeedback> feedbacks = orderFeedbackMapper.selectList(wrapper);
        feedbacks.sort(Comparator.comparing(OrderFeedback::getSubmittedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Set<Long> versionIds = feedbacks.stream()
                .map(OrderFeedback::getVersionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DesignVersion> versionMap = versionIds.isEmpty()
                ? Map.of()
                : designVersionMapper.selectBatchIds(versionIds).stream()
                        .collect(Collectors.toMap(DesignVersion::getId, v -> v));

        Map<Long, String> userNames = loadUserNames(
                feedbacks.stream().map(OrderFeedback::getSubmittedBy).collect(Collectors.toSet()));

        return feedbacks.stream()
                .map(f -> toFeedbackVO(f, versionMap, userNames))
                .collect(Collectors.toList());
    }

    private DesignVersion findLatestVersion(Long taskId) {
        List<DesignVersion> versions = designVersionMapper.selectList(
                new LambdaQueryWrapper<DesignVersion>().eq(DesignVersion::getTaskId, taskId));
        return versions.stream()
                .max(Comparator.comparingInt(v -> parseVersionNo(v.getVersionNo())))
                .orElse(null);
    }

    private static int parseVersionNo(String versionNo) {
        if (versionNo == null) {
            return 0;
        }
        Matcher matcher = VERSION_NO_PATTERN.matcher(versionNo);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private Order requireOrderForRead(Long orderId, Long taskId, Long userId, String userRole) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR,
                    "orderId 或 taskId 至少传一个");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (taskId != null) {
            OrderTask task = orderTaskMapper.selectById(taskId);
            if (task == null || !order.getId().equals(task.getOrderId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "物料任务不存在或不属于该订单");
            }
        }
        checkViewScope(order, taskId, userId, userRole);
        return order;
    }

    private void checkViewScope(Order order, Long taskId, Long userId, String userRole) {
        if (UserRole.BL.getCode().equals(userRole)) {
            return;
        }
        if (UserRole.AM.getCode().equals(userRole) && userId != null
                && userId.equals(order.getCreatedBy())) {
            return;
        }
        if (UserRole.DS.getCode().equals(userRole) && userId != null) {
            if (taskId != null) {
                OrderTask task = orderTaskMapper.selectById(taskId);
                if (task != null && userId.equals(task.getDesignerId())) {
                    return;
                }
            } else {
                Long assigned = orderTaskMapper.selectCount(
                        new LambdaQueryWrapper<OrderTask>()
                                .eq(OrderTask::getOrderId, order.getId())
                                .eq(OrderTask::getDesignerId, userId));
                if (assigned != null && assigned > 0) {
                    return;
                }
            }
        }
        throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看该订单的修改意见");
    }

    private Map<Long, String> loadUserNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    private OrderFeedbackVO toFeedbackVO(OrderFeedback feedback,
                                         Map<Long, DesignVersion> versionMap,
                                         Map<Long, String> userNames) {
        OrderFeedbackVO vo = new OrderFeedbackVO();
        vo.setId(feedback.getId());
        vo.setTaskId(feedback.getTaskId());
        vo.setContent(feedback.getContent());
        vo.setStatus(feedback.getStatus());
        vo.setVersionId(feedback.getVersionId());
        DesignVersion version = versionMap.get(feedback.getVersionId());
        vo.setVersionNo(version != null ? version.getVersionNo() : null);
        vo.setSubmittedByName(userNames.get(feedback.getSubmittedBy()));
        vo.setSubmittedAt(feedback.getSubmittedAt());
        return vo;
    }
}
