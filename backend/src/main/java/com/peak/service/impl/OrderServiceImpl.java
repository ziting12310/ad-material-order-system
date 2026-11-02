package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.FeedbackStatus;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
import com.peak.dto.OrderApproveRequest;
import com.peak.dto.OrderAssignRequest;
import com.peak.dto.OrderCancelRequest;
import com.peak.dto.OrderCreateRequest;
import com.peak.dto.OrderDeliverRequest;
import com.peak.dto.OrderRejectRequest;
import com.peak.dto.OrderUpdateRequest;
import com.peak.dto.VersionOnlyRequest;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.entity.OrderFeedback;
import com.peak.entity.OrderReferenceFile;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderFeedbackMapper;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.OrderReferenceFileMapper;
import com.peak.mapper.UserMapper;
import com.peak.service.OrderService;
import com.peak.vo.CursorPage;
import com.peak.vo.OrderApproveResponseVO;
import com.peak.vo.OrderAssignResponseVO;
import com.peak.vo.OrderCreateResponseVO;
import com.peak.vo.OrderDeliverResponseVO;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderListItemVO;
import com.peak.vo.OrderRejectResponseVO;
import com.peak.vo.OrderStatusUpdateResponseVO;
import com.peak.vo.ReferenceFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 订单核心服务实现 — API.md §3（覆盖 13 个 Domain Actions 中 11 个订单级动作）
 *
 * <p>职责边界：
 *  - 业务校验（存在性 / 权限 / 状态流转前置 / 乐观锁）
 *  - 状态推进（调用 MP updateById 触发 @Version 乐观锁插件）
 *  - VO 组装（Entity → VO，联表 users.name 填 designerName / createdByName）
 *  - 不写 SQL，全部委托 Mapper
 *  - 敏感字段过滤（passwordHash 不组装进 VO）
 * </p>
 *
 * <p>乐观锁拦截：MP OptimisticLockerInnerInterceptor 插件在 updateById 时自动
 * 追加 WHERE version = ?，更新成功后 SET version = version + 1。
 * 版本不匹配时 updateById 返回影响行数 0 → 本 ServiceImpl 检查后抛 ORDER_VERSION_CONFLICT。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // ---------- Mapper 注入 ----------
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final DesignVersionMapper designVersionMapper;
    private final OrderFeedbackMapper orderFeedbackMapper;
    private final OrderReferenceFileMapper orderReferenceFileMapper;

    // ========================================================================
    // 【1】listOrders — API.md §3.1 订单列表（Cursor 游标分页）
    // ========================================================================
    @Override
    public CursorPage<OrderListItemVO> listOrders(Long userId, String userRole,
                                                   String cursor, int limit,
                                                   String materialType, String status, String keyword) {
        // ① 解析游标 → lastId（游标为 null 时，查 created_at DESC, id DESC 首页）
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        // ② 构建查询条件（LambdaQueryWrapper — 驼峰映射自动生效）
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Order::getId, lastId);              // 游标条件：上次最后一条的 ID 之后
        wrapper.orderByDesc(Order::getCreatedAt);
        wrapper.orderByDesc(Order::getId);
        wrapper.last("LIMIT " + (effectiveLimit + 1)); // 多查 1 条判断 hasMore

        // ③ 数据范围过滤（角色维度）
        applyDataScopeFilter(wrapper, userId, userRole);

        // ④ 可选条件
        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(Order::getMaterialType, materialType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Order::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            // XSS 清洗关键字（API.md §3.1 搜索特殊字符 XSS 防护）
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            wrapper.and(w -> w.like(Order::getOrderNo, safeKeyword)
                    .or().like(Order::getCustomerName, safeKeyword));
        }

        // ⑤ 执行查询
        List<Order> all = orderMapper.selectList(wrapper);
        boolean hasMore = all.size() > effectiveLimit;
        List<Order> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        // ⑥ 联表查 designer 姓名（批量查询避免 N+1）
        Map<Long, String> designerNames = batchLoadUserNames(
                page.stream().map(Order::getDesignerId).filter(id -> id != null).distinct().toList());

        // ⑦ 组装 VO
        List<OrderListItemVO> voList = page.stream()
                .map(o -> buildListItemVO(o, designerNames.get(o.getDesignerId())))
                .toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    // ========================================================================
    // 【2】getOrderDetail — API.md §3.3 订单详情（聚合子实体）
    // ========================================================================
    @Override
    public OrderDetailVO getOrderDetail(Long orderId, Long userId, String userRole) {
        // ① 查订单 → 不存在抛异常
        Order order = findOrderOrThrow(orderId);

        // ② 数据范围校验（非创建人 / 非设计人 / 非 BL → 拒绝）
        checkDataScope(order, userId, userRole);

        // ③ 查关联数据（批量联表，避免 N+1）
        User designer = order.getDesignerId() != null ? userMapper.selectById(order.getDesignerId()) : null;
        User creator = order.getCreatedBy() != null ? userMapper.selectById(order.getCreatedBy()) : null;

        List<OrderReferenceFile> refFiles = orderReferenceFileMapper.selectList(
                new LambdaQueryWrapper<OrderReferenceFile>().eq(OrderReferenceFile::getOrderId, orderId)
        );
        List<DesignVersion> versions = designVersionMapper.selectList(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getOrderId, orderId)
                        .orderByAsc(DesignVersion::getVersionNo)
        );
        List<OrderFeedback> feedbacks = orderFeedbackMapper.selectList(
                new LambdaQueryWrapper<OrderFeedback>()
                        .eq(OrderFeedback::getOrderId, orderId)
                        .orderByAsc(OrderFeedback::getSubmittedAt)
        );
        // feedback 的 submitter 姓名 + version 的 uploader 姓名批量查
        // 注：Stream.concat 是静态方法，两个 stream 均为有限流，顺序无关（只取 id 集合）
        Map<Long, String> allUserNames = batchLoadUserNames(
                Stream.concat(
                                feedbacks.stream().map(OrderFeedback::getSubmittedBy),
                                versions.stream().map(DesignVersion::getUploadedBy)
                        )
                        .filter(id -> id != null).distinct().toList()
        );

        // ④ 组装 OrderDetailVO（敏感字段不返回）
        return buildDetailVO(order, designer, creator, refFiles, versions, feedbacks, allUserNames);
    }

    // ========================================================================
    // 【3】createOrder — API.md §3.2 创建订单（WAIT_CREATE → CREATED）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponseVO createOrder(OrderCreateRequest dto, Long creatorId) {
        // ① 生成订单编号 ORD-YYYY-NNNNN（查当前年最大编号 + 1）
        String orderNo = generateOrderNo();

        // ② 组装 Order 实体（状态 CREATED，创建人 creatorId）
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomerName(StringEscapeUtils.escapeHtml4(dto.getCustomerName()));    // XSS 清洗
        order.setContactName(StringEscapeUtils.escapeHtml4(dto.getContactName()));
        order.setContactPhone(dto.getContactPhone());                                    // 手机号已用 @Pattern 校验
        order.setMaterialType(dto.getMaterialType());
        order.setQuantity(dto.getQuantity());
        order.setSpecification(StringEscapeUtils.escapeHtml4(dto.getSpecification()));
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setRemark(dto.getRemark() != null ? StringEscapeUtils.escapeHtml4(dto.getRemark()) : null);
        order.setStatus(OrderStatus.CREATED.getCode());
        order.setCreatedBy(creatorId);
        order.setArchived(0);
        order.setVersion(0);   // 乐观锁初值

        // ③ insert
        orderMapper.insert(order);

        // ④ 保存参考文件（同事务）
        if (dto.getReferenceFileIds() != null && !dto.getReferenceFileIds().isEmpty()) {
            for (String fileId : dto.getReferenceFileIds()) {
                OrderReferenceFile ref = new OrderReferenceFile();
                ref.setOrderId(order.getId());
                ref.setFileId(fileId);  // 占位，presign 接口返回的 fileId
                ref.setUploadedBy(creatorId);
                orderReferenceFileMapper.insert(ref);
            }
        }

        return buildCreateResponse(order);
    }

    // ========================================================================
    // 【4】updateOrder — API.md §3.4 编辑订单（仅 CREATED 状态可编辑）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponseVO updateOrder(Long orderId, OrderUpdateRequest dto, Long operatorId) {
        // ① 查订单 + 存在性校验
        Order order = findOrderOrThrow(orderId);

        // ② 状态校验：仅 CREATED 可编辑
        checkStatus(order, OrderStatus.CREATED, "编辑订单");

        // ③ 数据范围：AM 只能编辑自己创建的
        if (UserRole.AM.getCode().equals(getUserRole(operatorId))) {
            if (!operatorId.equals(order.getCreatedBy())) {
                throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权编辑他人订单");
            }
        }

        // ④ 乐观锁校验
        checkVersion(order, dto.getVersion());

        // ⑤ 更新字段（XSS 清洗）
        order.setCustomerName(StringEscapeUtils.escapeHtml4(dto.getCustomerName()));
        order.setContactName(StringEscapeUtils.escapeHtml4(dto.getContactName()));
        order.setContactPhone(dto.getContactPhone());
        order.setMaterialType(dto.getMaterialType());
        order.setQuantity(dto.getQuantity());
        order.setSpecification(StringEscapeUtils.escapeHtml4(dto.getSpecification()));
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setRemark(dto.getRemark() != null ? StringEscapeUtils.escapeHtml4(dto.getRemark()) : null);

        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        return buildCreateResponse(order);
    }

    // ========================================================================
    // 【5】assignOrder — API.md §3.5 分派设计人员（CREATED → ASSIGNED）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderAssignResponseVO assignOrder(Long orderId, OrderAssignRequest dto, Long operatorId) {
        // ① 查订单
        Order order = findOrderOrThrow(orderId);

        // ② 状态校验：仅 CREATED 可分派
        checkStatus(order, OrderStatus.CREATED, "分派设计");

        // ③ 权限校验：仅 BL 可分派
        requireRole(operatorId, UserRole.BL.getCode());

        // ④ 校验目标用户存在 + 是 DS 角色
        User designer = userMapper.selectById(dto.getDesignerId());
        if (designer == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "设计人员不存在");
        }
        if (!UserRole.DS.getCode().equals(designer.getRole())) {
            throw new BusinessException(ErrorCode.DESIGNER_ROLE_INVALID, "目标用户不是设计人员角色");
        }

        // ⑤ 乐观锁校验
        checkVersion(order, dto.getVersion());

        // ⑥ 执行分派：更新状态 + 设计人 + 分派人 + 分派时间
        order.setStatus(OrderStatus.ASSIGNED.getCode());
        order.setDesignerId(dto.getDesignerId());
        order.setAssignedBy(operatorId);
        order.setAssignedAt(LocalDateTime.now());

        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        return buildAssignResponse(order, designer);
    }

    // ========================================================================
    // 【6】acceptTask — API.md §3.6 设计师接单（ASSIGNED → DESIGNING）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderStatusUpdateResponseVO acceptTask(Long orderId, VersionOnlyRequest dto, Long designerId) {
        // ① 查订单
        Order order = findOrderOrThrow(orderId);

        // ② 状态校验：仅 ASSIGNED 可接单
        checkStatus(order, OrderStatus.ASSIGNED, "设计师接单");

        // ③ 权限校验：仅分派给自己的设计师可接单
        if (!designerId.equals(order.getDesignerId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "该订单未分派给您");
        }
        requireRole(designerId, UserRole.DS.getCode());

        // ④ 乐观锁校验
        checkVersion(order, dto.getVersion());

        // ⑤ 状态推进
        order.setStatus(OrderStatus.DESIGNING.getCode());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        return buildStatusResponse(order);
    }

    // ========================================================================
    // 【7】rejectDesign — API.md §3.7 驳回设计稿（WAIT_APPROVE → DESIGNING + 自动 Feedback）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRejectResponseVO rejectDesign(Long orderId, OrderRejectRequest dto, Long operatorId) {
        // ① 查订单
        Order order = findOrderOrThrow(orderId);

        // ② 状态校验：仅 WAIT_APPROVE 可驳回
        checkStatus(order, OrderStatus.WAIT_APPROVE, "驳回设计");

        // ③ 权限校验：仅 BL 可驳回
        requireRole(operatorId, UserRole.BL.getCode());

        // ④ 乐观锁校验
        checkVersion(order, dto.getVersion());

        // ⑤ 更新订单状态
        order.setStatus(OrderStatus.DESIGNING.getCode());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改");
        }

        // ⑥ 自动创建一条 PENDING 状态的修改意见（驳回原因作为意见内容）
        OrderFeedback feedback = new OrderFeedback();
        feedback.setOrderId(order.getId());
        feedback.setVersionId(order.getFinalVersionId());  // 驳回针对最近上传的版本
        feedback.setContent(StringEscapeUtils.escapeHtml4(dto.getRejectNote()));
        feedback.setStatus(FeedbackStatus.PENDING.getCode());
        feedback.setSubmittedBy(operatorId);
        feedback.setSubmittedAt(LocalDateTime.now());
        orderFeedbackMapper.insert(feedback);

        // ⑦ 组装响应
        OrderRejectResponseVO resp = new OrderRejectResponseVO();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setFeedbackId(feedback.getId());
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【8】approveFinal — API.md §3.8 确认终稿（WAIT_APPROVE → APPROVED + 标记 final）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderApproveResponseVO approveFinal(Long orderId, OrderApproveRequest dto, Long operatorId) {
        // ① 查订单
        Order order = findOrderOrThrow(orderId);

        // ② 状态校验：仅 WAIT_APPROVE 可定稿
        checkStatus(order, OrderStatus.WAIT_APPROVE, "确认终稿");

        // ③ 权限校验：仅 BL 可定稿
        requireRole(operatorId, UserRole.BL.getCode());

        // ④ 乐观锁校验
        checkVersion(order, dto.getVersion());

        // ⑤ 查订单最新上传的设计稿版本（标记为 final）
        DesignVersion latest = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getOrderId, orderId)
                        .orderByDesc(DesignVersion::getId)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND, "设计稿版本不存在");
        }
        latest.setFinalVersion(1);
        designVersionMapper.updateById(latest);

        // ⑥ 处理所有 PENDING 状态的 Feedback（置 PROCESSED + processed_at）
        LocalDateTime now = LocalDateTime.now();
        orderFeedbackMapper.update(null,
                new LambdaUpdateWrapper<OrderFeedback>()
                        .eq(OrderFeedback::getOrderId, orderId)
                        .eq(OrderFeedback::getStatus, FeedbackStatus.PENDING.getCode())
                        .set(OrderFeedback::getStatus, FeedbackStatus.PROCESSED.getCode())
                        .set(OrderFeedback::getProcessedAt, now)
        );

        // ⑦ 更新订单状态 + 定稿人 + 定稿时间 + 定稿版本
        order.setStatus(OrderStatus.APPROVED.getCode());
        order.setApprovedBy(operatorId);
        order.setApprovedAt(now);
        order.setFinalVersionId(latest.getId());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改");
        }

        // ⑧ 组装响应
        OrderApproveResponseVO resp = new OrderApproveResponseVO();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setFinalVersionId(latest.getId());
        resp.setApprovedAt(now);
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【9】markDelivered — API.md §3.9 标记交付（APPROVED → DELIVERED + 归档 archived=1）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDeliverResponseVO markDelivered(Long orderId, OrderDeliverRequest dto, Long operatorId) {
        // ① 查订单
        Order order = findOrderOrThrow(orderId);

        // ② 状态校验：仅 APPROVED 可交付
        checkStatus(order, OrderStatus.APPROVED, "标记交付");

        // ③ 权限校验：仅 BL 可交付
        requireRole(operatorId, UserRole.BL.getCode());

        // ④ 乐观锁校验
        checkVersion(order, dto.getVersion());

        // ⑤ 更新订单
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.DELIVERED.getCode());
        order.setDeliveredBy(operatorId);
        order.setDeliveredAt(now);
        order.setDeliverRemark(dto.getDeliverRemark());
        order.setArchived(1);   // 自动归档（ARCH §5.3 PER-005）
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改");
        }

        // ⑥ 组装响应
        OrderDeliverResponseVO resp = new OrderDeliverResponseVO();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setDeliveredAt(now);
        resp.setIsArchived(true);
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【10】cancelOrder — API.md §3.10 作废订单（任意非终态 → CANCELLED）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderStatusUpdateResponseVO cancelOrder(Long orderId, OrderCancelRequest dto, Long operatorId) {
        // ① 查订单
        Order order = findOrderOrThrow(orderId);

        // ② 终态拦截（DELIVERED / CANCELLED 不能再作废）
        if (OrderStatus.DELIVERED.getCode().equals(order.getStatus())
                || OrderStatus.CANCELLED.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "已交付或已作废的订单不能再作废");
        }

        // ③ 权限校验：BL 可作废任意 / AM 只能作废自己创建的
        String role = getUserRole(operatorId);
        if (UserRole.AM.getCode().equals(role) && !operatorId.equals(order.getCreatedBy())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权作废他人订单");
        }

        // ④ 乐观锁校验
        checkVersion(order, dto.getVersion());

        // ⑤ 状态推进
        order.setStatus(OrderStatus.CANCELLED.getCode());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改");
        }

        return buildStatusResponse(order);
    }

    // ========================================================================
    //  私有辅助方法 — 避免代码重复
    // ========================================================================

    /** 查订单 + 不存在抛 404 */
    private Order findOrderOrThrow(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /** 状态流转前置校验（当前状态 ≠ 期望值 → 抛 ORDER_STATUS_CONFLICT 422） */
    private void checkStatus(Order order, OrderStatus expected, String actionLabel) {
        if (!expected.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前订单状态为 [%s]，%s 需要状态为 [%s]",
                    OrderStatus.of(order.getStatus()) != null ? OrderStatus.of(order.getStatus()).getLabel() : order.getStatus(),
                    actionLabel,
                    expected.getLabel());
        }
    }

    /** 乐观锁版本校验（前端 version vs DB version 不一致 → 抛 ORDER_VERSION_CONFLICT 409） */
    private void checkVersion(Order order, Integer requestVersion) {
        if (!order.getVersion().equals(requestVersion)) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "数据已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    order.getVersion(), requestVersion);
        }
    }

    /** 角色权限校验（当前用户角色 ≠ 所需角色 → 抛 PERMISSION_DENIED 403） */
    private void requireRole(Long userId, String requiredRole) {
        String actualRole = getUserRole(userId);
        if (!requiredRole.equals(actualRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "需要 %s 角色才能执行该操作", UserRole.of(requiredRole).getLabel());
        }
    }

    /** 获取用户角色（查 users 表） */
    private String getUserRole(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null ? u.getRole() : null;
    }

    /** 数据范围过滤（按角色加 Wrapper 条件） */
    private void applyDataScopeFilter(LambdaQueryWrapper<Order> wrapper, Long userId, String role) {
        if (UserRole.AM.getCode().equals(role)) {
            wrapper.eq(Order::getCreatedBy, userId);    // AM：只看自己创建的
        } else if (UserRole.DS.getCode().equals(role)) {
            wrapper.eq(Order::getDesignerId, userId);  // DS：只看分派给自己的
        }
        // BL：不加过滤，看全部
    }

    /** 数据范围校验（详情查询） */
    private void checkDataScope(Order order, Long userId, String role) {
        if (UserRole.AM.getCode().equals(role)) {
            if (!userId.equals(order.getCreatedBy())) {
                throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看他人订单");
            }
        } else if (UserRole.DS.getCode().equals(role)) {
            if (!userId.equals(order.getDesignerId())) {
                throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看非自己的订单");
            }
        }
        // BL：全量访问
    }

    /** Cursor 编码：Base64(id) */
    private String encodeCursor(long lastId) {
        return Base64.getEncoder().encodeToString(String.valueOf(lastId).getBytes());
    }

    /** Cursor 解码：Base64 → lastId（null / 空 → 0） */
    private long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            return Long.parseLong(new String(Base64.getDecoder().decode(cursor)));
        } catch (Exception e) {
            log.warn("Cursor 解码失败：{}，重置为首页", cursor);
            return 0;
        }
    }

    /** 订单编号生成：ORD-YYYY-NNNNN（查当前年最大编号 + 1） */
    private String generateOrderNo() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "ORD-" + year + "-";
        // 查当前年最大编号（ORD-2026-xxxxx → 取 xxxxx 部分转 int + 1）
        List<Order> yearOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().likeRight(Order::getOrderNo, prefix)
        );
        int nextSeq = 1;
        if (!yearOrders.isEmpty()) {
            int maxSeq = yearOrders.stream()
                    .mapToInt(o -> {
                        try {
                            return Integer.parseInt(o.getOrderNo().substring(prefix.length()));
                        } catch (Exception e) { return 0; }
                    }).max().orElse(0);
            nextSeq = maxSeq + 1;
        }
        return prefix + String.format("%05d", nextSeq);
    }

    /** 批量查用户姓名（避免 N+1） */
    private Map<Long, String> batchLoadUserNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> result = new HashMap<>();
        users.forEach(u -> result.put(u.getId(), u.getName()));
        return result;
    }

    // ---------- VO 组装 ----------

    private OrderListItemVO buildListItemVO(Order order, String designerName) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerName(order.getCustomerName());
        vo.setMaterialType(order.getMaterialType());
        vo.setQuantity(order.getQuantity());
        vo.setStatus(order.getStatus());
        vo.setDeliveryDate(order.getDeliveryDate());
        vo.setDesignerName(designerName);
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private OrderCreateResponseVO buildCreateResponse(Order order) {
        OrderCreateResponseVO vo = new OrderCreateResponseVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private OrderAssignResponseVO buildAssignResponse(Order order, User designer) {
        OrderAssignResponseVO vo = new OrderAssignResponseVO();
        vo.setId(order.getId());
        vo.setStatus(order.getStatus());
        vo.setDesignerId(designer.getId());
        vo.setDesignerName(designer.getName());
        vo.setAssignedAt(order.getAssignedAt());
        vo.setVersion(order.getVersion());
        return vo;
    }

    private OrderStatusUpdateResponseVO buildStatusResponse(Order order) {
        OrderStatusUpdateResponseVO vo = new OrderStatusUpdateResponseVO();
        vo.setId(order.getId());
        vo.setStatus(order.getStatus());
        vo.setVersion(order.getVersion());
        return vo;
    }

    private OrderDetailVO buildDetailVO(Order order, User designer, User creator,
                                        List<OrderReferenceFile> refFiles,
                                        List<DesignVersion> versions,
                                        List<OrderFeedback> feedbacks,
                                        Map<Long, String> allUserNames) {
        OrderDetailVO vo = new OrderDetailVO();
        // 主信息（敏感字段 passwordHash / is_deleted 不返回）
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerName(order.getCustomerName());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setMaterialType(order.getMaterialType());
        vo.setQuantity(order.getQuantity());
        vo.setSpecification(order.getSpecification());
        vo.setDeliveryDate(order.getDeliveryDate());
        vo.setRemark(order.getRemark());
        vo.setStatus(order.getStatus());
        vo.setDesignerId(order.getDesignerId());
        vo.setDesignerName(designer != null ? designer.getName() : null);
        vo.setCreatedByName(creator != null ? creator.getName() : null);
        vo.setCreatedAt(order.getCreatedAt());
        vo.setAssignedAt(order.getAssignedAt());
        vo.setIsArchived(Integer.valueOf(1).equals(order.getArchived()));
        vo.setVersion(order.getVersion());

        // 子列表
        vo.setReferenceFiles(refFiles.stream().map(r -> {
            ReferenceFileVO rf = new ReferenceFileVO();
            rf.setId(r.getId());
            rf.setFileUrl(r.getFileUrl());
            rf.setFileName(r.getFileName());
            rf.setFileSize(r.getFileSize());
            return rf;
        }).toList());

        vo.setDesignVersions(versions.stream().map(v -> {
            com.peak.vo.DesignVersionVO dv = new com.peak.vo.DesignVersionVO();
            dv.setId(v.getId());
            dv.setVersionNo(v.getVersionNo());
            dv.setFileUrl(v.getFileUrl());
            dv.setFileName(v.getFileName());
            dv.setFileSize(v.getFileSize());
            dv.setVersionNote(v.getVersionNote());
            dv.setIsFinal(Integer.valueOf(1).equals(v.getFinalVersion()));
            dv.setUploadedByName(allUserNames.get(v.getUploadedBy()));
            dv.setUploadedAt(v.getUploadedAt());
            return dv;
        }).toList());

        // feedback 需关联 versionNo
        Map<Long, String> versionNoMap = versions.stream()
                .collect(Collectors.toMap(DesignVersion::getId, DesignVersion::getVersionNo));
        vo.setFeedbacks(feedbacks.stream().map(f -> {
            com.peak.vo.OrderFeedbackVO fb = new com.peak.vo.OrderFeedbackVO();
            fb.setId(f.getId());
            fb.setContent(f.getContent());
            fb.setStatus(f.getStatus());
            fb.setVersionId(f.getVersionId());
            fb.setVersionNo(versionNoMap.get(f.getVersionId()));
            fb.setSubmittedByName(allUserNames.get(f.getSubmittedBy()));
            fb.setSubmittedAt(f.getSubmittedAt());
            return fb;
        }).toList());

        return vo;
    }
}
