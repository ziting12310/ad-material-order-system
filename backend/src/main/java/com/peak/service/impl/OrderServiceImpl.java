package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.FeedbackStatus;
import com.peak.common.enums.MaterialType;
import com.peak.common.enums.OperationType;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.OrderStatusEnum;
import com.peak.common.enums.UserRole;
import com.peak.dto.OrderApproveRequest;
import com.peak.dto.OrderAssignRequest;
import com.peak.dto.OrderCancelRequest;
import com.peak.dto.OrderConfirmDTO;
import com.peak.dto.OrderCreateRequest;
import com.peak.dto.OrderDeliverRequest;
import com.peak.dto.OrderRejectRequest;
import com.peak.dto.OrderUpdateRequest;
import com.peak.dto.TaskCreateItem;
import com.peak.dto.VersionOnlyRequest;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.entity.OrderFeedback;
import com.peak.entity.OrderOperationLog;
import com.peak.entity.OrderReferenceFile;
import com.peak.entity.OrderTask;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderFeedbackMapper;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.OrderOperationLogMapper;
import com.peak.mapper.OrderReferenceFileMapper;
import com.peak.mapper.OrderTaskMapper;
import com.peak.mapper.UserMapper;
import com.peak.service.OrderService;
import com.peak.service.OrderTaskService;
import com.peak.util.OperationLogUtil;
import com.peak.util.OrderStateMachineUtil;
import com.peak.vo.CursorPage;
import com.peak.vo.DesignVersionVO;
import com.peak.vo.OperationLogVO;
import com.peak.vo.OrderApproveResponseVO;
import com.peak.vo.OrderAssignResponseVO;
import com.peak.vo.OrderCreateResponseVO;
import com.peak.vo.OrderDeliverResponseVO;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderFeedbackVO;
import com.peak.vo.OrderListItemVO;
import com.peak.vo.OrderRejectResponseVO;
import com.peak.vo.OrderStatusUpdateResponseVO;
import com.peak.vo.OrderTaskVO;
import com.peak.vo.ReferenceFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 订单核心服务实现 — 多物料模型
 *
 * <p>职责边界：
 *  - 订单级 CRUD + 状态流转（创建/确认/交付/作废）
 *  - 订单状态由所有物料任务状态聚合而来
 *  - 操作日志记录
 *  - 归档只读全链路校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final DesignVersionMapper designVersionMapper;
    private final OrderFeedbackMapper orderFeedbackMapper;
    private final OrderReferenceFileMapper orderReferenceFileMapper;
    private final OrderTaskMapper orderTaskMapper;
    private final OrderOperationLogMapper operationLogMapper;
    private final OperationLogUtil opLog;

    @Autowired
    @Lazy
    private OrderTaskService orderTaskService;

    // ========================================================================
    // 【状态机接入】对外 4 态校验（内部 7 态投影后执行）
    // 约定：4 态对外、7 态内部流转；两处校验均插在原有 checkStatus 之后、写 DB 之前，
    //      不替换原有逻辑，只做增量守门，非法流转抛 422。
    // ========================================================================
    /**
     * 状态跃迁校验（顺序流转守门）
     *
     * <p>先把内部 7 态投影为对外 4 态（{@link OrderStatusEnum#fromInternal}），
     * 再交给 {@link OrderStateMachineUtil#transition} 校验
     * CREATED→ASSIGNED→REVIEWING→FINISHED 是否合法。</p>
     *
     * <p>CANCELLED 等无对外映射的内部态直接跳过（返回 null），继续由原有 checkStatus 兜底。</p>
     */
    private void checkExternalTransition(Order order, OrderStatusEnum target) {
        OrderStatusEnum current = OrderStatusEnum.fromInternal(order.getStatus());
        if (current == null) {
            return;   // 无对外口径（如 CANCELLED），保持老逻辑
        }
        OrderStateMachineUtil.transition(current.getCode(), target);
    }

    /**
     * 状态守卫（不改变状态、仅特定对外状态下可操作，如编辑订单仅 CREATED）
     */
    private void checkExternalState(Order order, OrderStatusEnum expected, String actionLabel) {
        OrderStatusEnum current = OrderStatusEnum.fromInternal(order.getStatus());
        if (current == null) {
            return;   // 无对外口径（如 CANCELLED），保持老逻辑
        }
        OrderStateMachineUtil.requireState(current.getCode(), expected, actionLabel);
    }

    // ========================================================================
    // 【1】订单列表（Cursor 游标分页）
    // ========================================================================
    @Override
    public CursorPage<OrderListItemVO> listOrders(Long userId, String userRole,
                                                   String cursor, int limit,
                                                   String materialType, String status, String keyword,
                                                   Long designerId, Long creatorId,
                                                   String deliveryDateFrom, String deliveryDateTo) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Order::getId, lastId);
        wrapper.orderByDesc(Order::getCreatedAt);
        wrapper.orderByDesc(Order::getId);
        wrapper.last("LIMIT " + (effectiveLimit + 1));

        // 数据范围过滤
        applyDataScopeFilter(wrapper, userId, userRole);

        // 可选过滤条件
        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(Order::getMaterialType, materialType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Order::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            wrapper.and(w -> w.like(Order::getOrderNo, safeKeyword)
                    .or().like(Order::getCustomerName, safeKeyword));
        }
        if (designerId != null) {
            wrapper.eq(Order::getDesignerId, designerId);
        }
        if (creatorId != null) {
            wrapper.eq(Order::getCreatedBy, creatorId);
        }
        if (deliveryDateFrom != null && !deliveryDateFrom.isBlank()) {
            wrapper.ge(Order::getDeliveryDate, java.time.LocalDate.parse(deliveryDateFrom));
        }
        if (deliveryDateTo != null && !deliveryDateTo.isBlank()) {
            wrapper.le(Order::getDeliveryDate, java.time.LocalDate.parse(deliveryDateTo));
        }

        List<Order> all = orderMapper.selectList(wrapper);
        boolean hasMore = all.size() > effectiveLimit;
        List<Order> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        // 批量查设计师姓名
        Map<Long, String> designerNames = batchLoadUserNames(
                page.stream().map(Order::getDesignerId).filter(id -> id != null).distinct().toList());

        // 批量查每个订单的任务统计（总数 + 已定稿数）
        Map<Long, int[]> taskStats = batchLoadTaskStats(
                page.stream().map(Order::getId).toList());

        List<OrderListItemVO> voList = page.stream()
                .map(o -> {
                    OrderListItemVO vo = buildListItemVO(o, designerNames.get(o.getDesignerId()));
                    int[] stats = taskStats.getOrDefault(o.getId(), new int[]{0, 0});
                    vo.setTaskCount(stats[0]);
                    vo.setApprovedTaskCount(stats[1]);
                    return vo;
                })
                .toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    // ========================================================================
    // 【2】订单详情
    // ========================================================================
    @Override
    public OrderDetailVO getOrderDetail(Long orderId, Long userId, String userRole) {
        Order order = findOrderOrThrow(orderId);
        checkDataScope(order, userId, userRole);

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

        Map<Long, String> allUserNames = batchLoadUserNames(
                Stream.concat(
                                feedbacks.stream().map(OrderFeedback::getSubmittedBy),
                                versions.stream().map(DesignVersion::getUploadedBy)
                        )
                        .filter(id -> id != null).distinct().toList()
        );

        // 物料任务列表
        List<OrderTaskVO> tasks = orderTaskService.listTasks(orderId, userId, userRole);

        // 操作历史
        List<OperationLogVO> logs = listOperationLogs(orderId, userId, userRole);

        // 任务统计
        int taskCount = tasks.size();
        int approvedCount = (int) tasks.stream()
                .filter(t -> OrderStatus.APPROVED.getCode().equals(t.getStatus()))
                .count();

        OrderDetailVO vo = buildDetailVO(order, designer, creator, refFiles, versions, feedbacks, allUserNames);
        vo.setTasks(tasks);
        vo.setOperationLogs(logs);
        vo.setTaskCount(taskCount);
        vo.setApprovedTaskCount(approvedCount);
        return vo;
    }

    // ========================================================================
    // 【3】创建订单
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponseVO createOrder(OrderCreateRequest dto, Long creatorId) {
        String orderNo = generateOrderNo();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomerName(StringEscapeUtils.escapeHtml4(dto.getCustomerName()));
        order.setContactName(StringEscapeUtils.escapeHtml4(dto.getContactName()));
        order.setContactPhone(dto.getContactPhone());
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setRemark(dto.getRemark() != null ? StringEscapeUtils.escapeHtml4(dto.getRemark()) : null);
        order.setStatus(OrderStatus.CREATED.getCode());
        order.setCreatedBy(creatorId);
        order.setArchived(0);
        order.setVersion(0);

        // 兼容旧版：如果传了单物料字段，自动生成一个任务
        if (dto.getTasks() == null || dto.getTasks().isEmpty()) {
            if (dto.getMaterialType() != null && dto.getQuantity() != null && dto.getSpecification() != null) {
                order.setMaterialType(dto.getMaterialType());
                order.setQuantity(dto.getQuantity());
                order.setSpecification(dto.getSpecification());
            } else {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_ERROR, "物料任务列表不能为空");
            }
        } else {
            // 多物料：第一个物料作为订单主信息的冗余
            TaskCreateItem first = dto.getTasks().get(0);
            order.setMaterialType(first.getMaterialType());
            order.setQuantity(first.getQuantity());
            order.setSpecification(first.getSpecification());
        }

        orderMapper.insert(order);

        // 批量创建物料任务
        if (dto.getTasks() != null && !dto.getTasks().isEmpty()) {
            orderTaskService.batchCreateTasks(order.getId(), dto.getTasks(), creatorId);
        } else if (dto.getMaterialType() != null) {
            // 旧版单物料兼容
            TaskCreateItem item = new TaskCreateItem();
            item.setMaterialType(dto.getMaterialType());
            item.setQuantity(dto.getQuantity());
            item.setSpecification(dto.getSpecification());
            item.setDeadline(dto.getDeliveryDate());
            item.setRemark(dto.getRemark());
            orderTaskService.batchCreateTasks(order.getId(), Collections.singletonList(item), creatorId);
        }

        // 参考文件
        if (dto.getReferenceFileIds() != null && !dto.getReferenceFileIds().isEmpty()) {
            for (String fileId : dto.getReferenceFileIds()) {
                OrderReferenceFile ref = new OrderReferenceFile();
                ref.setOrderId(order.getId());
                ref.setFileId(fileId);
                ref.setUploadedBy(creatorId);
                orderReferenceFileMapper.insert(ref);
            }
        }

        // 操作日志
        opLog.logOrder(order.getId(), OperationType.ORDER_CREATE,
                "创建订单：" + order.getCustomerName(),
                creatorId, null, null);

        return buildCreateResponse(order);
    }

    // ========================================================================
    // 【4】编辑订单（仅 CREATED 状态可编辑订单基本信息）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponseVO updateOrder(Long orderId, OrderUpdateRequest dto, Long operatorId) {
        Order order = findOrderOrThrow(orderId);
        checkStatus(order, OrderStatus.CREATED, "编辑订单");
        checkExternalState(order, OrderStatusEnum.CREATED, "编辑订单");   // 状态机守卫：编辑仅对外 CREATED

        String role = getUserRole(operatorId);
        if (UserRole.AM.getCode().equals(role)) {
            if (!operatorId.equals(order.getCreatedBy())) {
                throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权编辑他人订单");
            }
        }

        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        order.setCustomerName(StringEscapeUtils.escapeHtml4(dto.getCustomerName()));
        order.setContactName(StringEscapeUtils.escapeHtml4(dto.getContactName()));
        order.setContactPhone(dto.getContactPhone());
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setRemark(dto.getRemark() != null ? StringEscapeUtils.escapeHtml4(dto.getRemark()) : null);

        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        opLog.logOrder(orderId, OperationType.ORDER_EDIT,
                "编辑订单基本信息",
                operatorId, null, null);

        return buildCreateResponse(order);
    }

    // ========================================================================
    // 【5】分派设计人员（订单级 → 兼容旧版单物料）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderAssignResponseVO assignOrder(Long orderId, OrderAssignRequest dto, Long operatorId) {
        Order order = findOrderOrThrow(orderId);
        checkStatus(order, OrderStatus.CREATED, "分派设计");
        checkExternalTransition(order, OrderStatusEnum.ASSIGNED);   // 状态机：对外 CREATED → ASSIGNED
        requireRole(operatorId, UserRole.BL.getCode());
        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        User designer = userMapper.selectById(dto.getDesignerId());
        if (designer == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "设计人员不存在");
        }
        if (!UserRole.DS.getCode().equals(designer.getRole())) {
            throw new BusinessException(ErrorCode.DESIGNER_ROLE_INVALID, "目标用户不是设计人员角色");
        }

        // 旧版兼容：订单级分派 = 分派给所有 CREATED 状态的任务
        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>()
                        .eq(OrderTask::getOrderId, orderId)
                        .eq(OrderTask::getStatus, OrderStatus.CREATED.getCode())
        );

        if (tasks.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "没有可分派的物料任务");
        }

        // 全部任务分派给同一设计师
        for (OrderTask task : tasks) {
            task.setStatus(OrderStatus.ASSIGNED.getCode());
            task.setDesignerId(dto.getDesignerId());
            task.setAssignedBy(operatorId);
            task.setAssignedAt(LocalDateTime.now());
            orderTaskMapper.updateById(task);
        }

        // 更新订单冗余字段
        order.setDesignerId(dto.getDesignerId());
        order.setAssignedBy(operatorId);
        order.setAssignedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.ASSIGNED.getCode());
        orderMapper.updateById(order);

        opLog.logOrder(orderId, OperationType.TASK_ASSIGN,
                "分派所有物料任务给：" + designer.getName(),
                operatorId, null, null);

        OrderAssignResponseVO resp = new OrderAssignResponseVO();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setDesignerId(designer.getId());
        resp.setDesignerName(designer.getName());
        resp.setAssignedAt(order.getAssignedAt());
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【6】设计师接单（订单级 → 兼容旧版）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderStatusUpdateResponseVO acceptTask(Long orderId, VersionOnlyRequest dto, Long designerId) {
        Order order = findOrderOrThrow(orderId);
        checkStatus(order, OrderStatus.ASSIGNED, "设计师接单");
        checkExternalTransition(order, OrderStatusEnum.REVIEWING);   // 状态机：对外 ASSIGNED → REVIEWING

        if (!designerId.equals(order.getDesignerId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "该订单未分派给您");
        }
        requireRole(designerId, UserRole.DS.getCode());

        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        // 接单 = 所有 ASSIGNED 且分派给自己的任务进入 DESIGNING
        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>()
                        .eq(OrderTask::getOrderId, orderId)
                        .eq(OrderTask::getStatus, OrderStatus.ASSIGNED.getCode())
                        .eq(OrderTask::getDesignerId, designerId)
        );

        for (OrderTask task : tasks) {
            task.setStatus(OrderStatus.DESIGNING.getCode());
            orderTaskMapper.updateById(task);
        }

        order.setStatus(OrderStatus.DESIGNING.getCode());
        orderMapper.updateById(order);

        return buildStatusResponse(order);
    }

    // ========================================================================
    // 【7】驳回设计稿（订单级兼容）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRejectResponseVO rejectDesign(Long orderId, OrderRejectRequest dto, Long operatorId) {
        Order order = findOrderOrThrow(orderId);
        checkStatus(order, OrderStatus.WAIT_APPROVE, "驳回设计");
        // 状态机守卫：驳回在对外口径中仍属 REVIEWING（内部 WAIT_APPROVE → DESIGNING）
        checkExternalState(order, OrderStatusEnum.REVIEWING, "驳回设计");
        requireAnyRole(operatorId, UserRole.BL.getCode(), UserRole.AM.getCode());
        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        // 驳回所有 WAIT_APPROVE 状态的任务
        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>()
                        .eq(OrderTask::getOrderId, orderId)
                        .eq(OrderTask::getStatus, OrderStatus.WAIT_APPROVE.getCode())
        );

        Long firstFeedbackId = null;
        for (OrderTask task : tasks) {
            task.setStatus(OrderStatus.DESIGNING.getCode());
            orderTaskMapper.updateById(task);

            // 每个任务各创建一条 feedback
            DesignVersion latest = designVersionMapper.selectOne(
                    new LambdaQueryWrapper<DesignVersion>()
                            .eq(DesignVersion::getTaskId, task.getId())
                            .orderByDesc(DesignVersion::getId)
                            .last("LIMIT 1")
            );

            OrderFeedback feedback = new OrderFeedback();
            feedback.setOrderId(orderId);
            feedback.setTaskId(task.getId());
            feedback.setVersionId(latest != null ? latest.getId() : null);
            feedback.setContent(StringEscapeUtils.escapeHtml4(dto.getRejectNote()));
            feedback.setStatus(FeedbackStatus.PENDING.getCode());
            feedback.setSubmittedBy(operatorId);
            feedback.setSubmittedAt(LocalDateTime.now());
            orderFeedbackMapper.insert(feedback);
            if (firstFeedbackId == null) firstFeedbackId = feedback.getId();
        }

        order.setStatus(OrderStatus.DESIGNING.getCode());
        orderMapper.updateById(order);

        OrderRejectResponseVO resp = new OrderRejectResponseVO();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setFeedbackId(firstFeedbackId);
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【8】确认终稿（订单级兼容）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderApproveResponseVO approveFinal(Long orderId, OrderApproveRequest dto, Long operatorId) {
        Order order = findOrderOrThrow(orderId);
        checkStatus(order, OrderStatus.WAIT_APPROVE, "确认终稿");
        checkExternalTransition(order, OrderStatusEnum.FINISHED);   // 状态机：对外 REVIEWING → FINISHED
        requireAnyRole(operatorId, UserRole.BL.getCode(), UserRole.AM.getCode());
        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        // 定稿所有 WAIT_APPROVE 任务
        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>()
                        .eq(OrderTask::getOrderId, orderId)
                        .eq(OrderTask::getStatus, OrderStatus.WAIT_APPROVE.getCode())
        );

        LocalDateTime now = LocalDateTime.now();
        Long firstFinalVersionId = null;

        for (OrderTask task : tasks) {
            DesignVersion latest = designVersionMapper.selectOne(
                    new LambdaQueryWrapper<DesignVersion>()
                            .eq(DesignVersion::getTaskId, task.getId())
                            .orderByDesc(DesignVersion::getId)
                            .last("LIMIT 1")
            );
            if (latest != null) {
                latest.setFinalVersion(1);
                designVersionMapper.updateById(latest);
                if (firstFinalVersionId == null) firstFinalVersionId = latest.getId();
            }

            task.setStatus(OrderStatus.APPROVED.getCode());
            task.setApprovedBy(operatorId);
            task.setApprovedAt(now);
            task.setFinalVersionId(latest != null ? latest.getId() : null);
            task.setApproveRemark(dto.getApproveNote() != null
                    ? StringEscapeUtils.escapeHtml4(dto.getApproveNote()) : null);
            orderTaskMapper.updateById(task);

            // 处理该任务的 PENDING feedback
            orderFeedbackMapper.update(null,
                    new LambdaUpdateWrapper<OrderFeedback>()
                            .eq(OrderFeedback::getTaskId, task.getId())
                            .eq(OrderFeedback::getStatus, FeedbackStatus.PENDING.getCode())
                            .set(OrderFeedback::getStatus, FeedbackStatus.PROCESSED.getCode())
                            .set(OrderFeedback::getProcessedAt, now)
            );
        }

        // 更新订单
        order.setStatus(OrderStatus.APPROVED.getCode());
        order.setApprovedBy(operatorId);
        order.setApprovedAt(now);
        order.setFinalVersionId(firstFinalVersionId);
        orderMapper.updateById(order);

        OrderApproveResponseVO resp = new OrderApproveResponseVO();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setFinalVersionId(firstFinalVersionId);
        resp.setApprovedAt(now);
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【9】标记交付（APPROVED → DELIVERED + 归档）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDeliverResponseVO markDelivered(Long orderId, OrderDeliverRequest dto, Long operatorId) {
        Order order = findOrderOrThrow(orderId);

        // 核心校验：所有物料任务必须全部定稿
        boolean allApproved = orderTaskService.allTasksApproved(orderId);
        if (!allApproved) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "存在未完成的物料任务，全部定稿后才能交付归档");
        }

        requireRole(operatorId, UserRole.BL.getCode());
        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        LocalDateTime now = LocalDateTime.now();
        // 状态机守卫：交付前对外口径必须是 FINISHED（内部 APPROVED → DELIVERED）
        checkExternalState(order, OrderStatusEnum.FINISHED, "标记交付");
        order.setStatus(OrderStatus.DELIVERED.getCode());
        order.setDeliveredBy(operatorId);
        order.setDeliveredAt(now);
        order.setDeliverRemark(dto.getDeliverRemark() != null
                ? StringEscapeUtils.escapeHtml4(dto.getDeliverRemark()) : null);
        order.setArchived(1);

        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改，请刷新后重试");
        }

        opLog.logOrder(orderId, OperationType.ORDER_DELIVER,
                "交付归档：" + (dto.getDeliverRemark() != null ? dto.getDeliverRemark() : ""),
                operatorId, null, null);

        OrderDeliverResponseVO resp = new OrderDeliverResponseVO();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setDeliveredAt(now);
        resp.setIsArchived(true);
        resp.setVersion(order.getVersion());
        return resp;
    }

    // ========================================================================
    // 【10】作废订单
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderStatusUpdateResponseVO cancelOrder(Long orderId, OrderCancelRequest dto, Long operatorId) {
        Order order = findOrderOrThrow(orderId);

        if (OrderStatus.DELIVERED.getCode().equals(order.getStatus())
                || OrderStatus.CANCELLED.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "已交付或已作废的订单不能再作废");
        }

        String role = getUserRole(operatorId);
        if (UserRole.AM.getCode().equals(role) && !operatorId.equals(order.getCreatedBy())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权作废他人订单");
        }

        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        // 作废所有未完成任务
        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>()
                        .eq(OrderTask::getOrderId, orderId)
                        .ne(OrderTask::getStatus, OrderStatus.CANCELLED.getCode())
        );
        for (OrderTask task : tasks) {
            task.setStatus(OrderStatus.CANCELLED.getCode());
            orderTaskMapper.updateById(task);
        }

        // 取消是业务兜底动作（非终态均可作废），对外 4 态无 CANCELLED 口径，
        // 故豁免状态机顺序校验，仍由上方终态判断 + 原有逻辑兜底。
        order.setStatus(OrderStatus.CANCELLED.getCode());
        orderMapper.updateById(order);

        opLog.logOrder(orderId, OperationType.ORDER_CANCEL,
                "作废订单",
                operatorId, null, null);

        return buildStatusResponse(order);
    }

    // ========================================================================
    // 【11】同步订单状态（由任务状态变化触发）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncOrderStatus(Long orderId, Long operatorId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;

        // 终态不回退
        if (OrderStatus.DELIVERED.getCode().equals(order.getStatus())
                || OrderStatus.CANCELLED.getCode().equals(order.getStatus())) {
            return;
        }

        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>().eq(OrderTask::getOrderId, orderId)
        );
        if (tasks.isEmpty()) return;

        String aggregated = aggregateStatus(tasks);
        if (!aggregated.equals(order.getStatus())) {
            order.setStatus(aggregated);
            orderMapper.updateById(order);
        }
    }

    /**
     * 状态聚合规则（取"最远进展"）：
     *  全部 APPROVED → APPROVED
     *  任意 WAIT_APPROVE → WAIT_APPROVE
     *  任意 DESIGNING → DESIGNING
     *  任意 ASSIGNED → ASSIGNED
     *  全部 CREATED → CREATED
     *  （CANCELLED 任务忽略，不影响聚合）
     */
    private String aggregateStatus(List<OrderTask> tasks) {
        boolean allApproved = true;
        boolean hasWaitApprove = false;
        boolean hasDesigning = false;
        boolean hasAssigned = false;
        boolean hasActive = false;

        for (OrderTask task : tasks) {
            String s = task.getStatus();
            if (OrderStatus.CANCELLED.getCode().equals(s)) continue;
            hasActive = true;

            if (OrderStatus.APPROVED.getCode().equals(s)) {
                // 已定稿，不影响"进行中"判断
            } else if (OrderStatus.WAIT_APPROVE.getCode().equals(s)) {
                hasWaitApprove = true;
                allApproved = false;
            } else if (OrderStatus.DESIGNING.getCode().equals(s)) {
                hasDesigning = true;
                allApproved = false;
            } else if (OrderStatus.ASSIGNED.getCode().equals(s)) {
                hasAssigned = true;
                allApproved = false;
            } else {
                allApproved = false; // CREATED 或其他
            }
        }

        if (!hasActive) return OrderStatus.CANCELLED.getCode();
        if (allApproved) return OrderStatus.APPROVED.getCode();
        if (hasWaitApprove) return OrderStatus.WAIT_APPROVE.getCode();
        if (hasDesigning) return OrderStatus.DESIGNING.getCode();
        if (hasAssigned) return OrderStatus.ASSIGNED.getCode();
        return OrderStatus.CREATED.getCode();
    }

    // ========================================================================
    // 【12】BL 确认物料清单
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId, OrderConfirmDTO dto, Long operatorId) {
        Order order = findOrderOrThrow(orderId);
        checkStatus(order, OrderStatus.CREATED, "确认物料清单");
        requireRole(operatorId, UserRole.BL.getCode());
        checkNotArchived(order);
        checkVersion(order, dto.getVersion());

        // 用新的任务列表替换原有任务（仅 CREATED 状态任务可替换）
        // 先删除旧任务
        List<OrderTask> oldTasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>().eq(OrderTask::getOrderId, orderId)
        );
        for (OrderTask t : oldTasks) {
            orderTaskMapper.deleteById(t.getId());
        }

        // 批量创建新任务
        orderTaskService.batchCreateTasks(orderId, dto.getTasks(), operatorId);

        opLog.logOrder(orderId, OperationType.ORDER_CONFIRM,
                "确认物料清单（共 " + dto.getTasks().size() + " 项任务）",
                operatorId, null, null);

        // 确认后状态保持 CREATED，但有操作日志记录确认动作
    }

    // ========================================================================
    // 【13】操作历史查询
    // ========================================================================
    @Override
    public List<OperationLogVO> listOperationLogs(Long orderId, Long userId, String userRole) {
        Order order = findOrderOrThrow(orderId);
        checkDataScope(order, userId, userRole);

        List<OrderOperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OrderOperationLog>()
                        .eq(OrderOperationLog::getOrderId, orderId)
                        .orderByDesc(OrderOperationLog::getOperatedAt)
                        .orderByDesc(OrderOperationLog::getId)
        );

        // 查 taskNo 映射
        List<Long> taskIds = logs.stream()
                .map(OrderOperationLog::getTaskId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> taskNoMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<OrderTask> tasks = orderTaskMapper.selectBatchIds(taskIds);
            for (OrderTask t : tasks) {
                taskNoMap.put(t.getId(), t.getTaskNo());
            }
        }

        return logs.stream().map(l -> {
            OperationLogVO vo = new OperationLogVO();
            vo.setId(l.getId());
            vo.setTaskId(l.getTaskId());
            vo.setTaskNo(taskNoMap.get(l.getTaskId()));
            vo.setOperationType(l.getOperationType());
            OperationType ot = OperationType.of(l.getOperationType());
            vo.setOperationTypeName(ot != null ? ot.getLabel() : l.getOperationType());
            vo.setDescription(l.getDescription());
            vo.setOperatorId(l.getOperatorId());
            vo.setOperatorName(l.getOperatorName());
            vo.setOperatorRole(l.getOperatorRole());
            UserRole ur = UserRole.of(l.getOperatorRole());
            vo.setOperatorRoleName(ur != null ? ur.getLabel() : l.getOperatorRole());
            vo.setOperatedAt(l.getOperatedAt());
            return vo;
        }).toList();
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    private Order findOrderOrThrow(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void checkStatus(Order order, OrderStatus expected, String actionLabel) {
        if (!expected.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前订单状态为 [%s]，%s 需要状态为 [%s]",
                    OrderStatus.of(order.getStatus()) != null ? OrderStatus.of(order.getStatus()).getLabel() : order.getStatus(),
                    actionLabel,
                    expected.getLabel());
        }
    }

    private void checkVersion(Order order, Integer requestVersion) {
        if (!order.getVersion().equals(requestVersion)) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "数据已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    order.getVersion(), requestVersion);
        }
    }

    private void requireRole(Long userId, String requiredRole) {
        String actualRole = getUserRole(userId);
        if (!requiredRole.equals(actualRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "需要 %s 角色才能执行该操作", UserRole.of(requiredRole).getLabel());
        }
    }

    /** 多角色任一满足即可 */
    private void requireAnyRole(Long userId, String... allowedRoles) {
        String actualRole = getUserRole(userId);
        if (actualRole == null) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "用户不存在");
        }
        for (String r : allowedRoles) {
            if (r.equals(actualRole)) return;
        }
        StringBuilder labels = new StringBuilder();
        for (int i = 0; i < allowedRoles.length; i++) {
            if (i > 0) labels.append("/");
            UserRole ur = UserRole.of(allowedRoles[i]);
            labels.append(ur != null ? ur.getLabel() : allowedRoles[i]);
        }
        throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                "需要 %s 角色才能执行该操作", labels);
    }

    private String getUserRole(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null ? u.getRole() : null;
    }

    private void applyDataScopeFilter(LambdaQueryWrapper<Order> wrapper, Long userId, String role) {
        if (UserRole.AM.getCode().equals(role)) {
            wrapper.eq(Order::getCreatedBy, userId);
        } else if (UserRole.DS.getCode().equals(role)) {
            // DS：看分派给自己任务的订单（通过 task 子查询，这里简化用 designerId 冗余字段）
            wrapper.eq(Order::getDesignerId, userId);
        }
    }

    private void checkDataScope(Order order, Long userId, String role) {
        if (UserRole.AM.getCode().equals(role)) {
            if (!userId.equals(order.getCreatedBy())) {
                throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看他人订单");
            }
        } else if (UserRole.DS.getCode().equals(role)) {
            if (!userId.equals(order.getDesignerId())) {
                // 再查任务表确认
                Long count = orderTaskMapper.selectCount(
                        new LambdaQueryWrapper<OrderTask>()
                                .eq(OrderTask::getOrderId, order.getId())
                                .eq(OrderTask::getDesignerId, userId)
                );
                if (count == 0) {
                    throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看非自己的订单");
                }
            }
        }
    }

    private void checkNotArchived(Order order) {
        if (Integer.valueOf(1).equals(order.getArchived())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单已归档，不可修改");
        }
    }

    private String encodeCursor(long lastId) {
        return Base64.getEncoder().encodeToString(String.valueOf(lastId).getBytes());
    }

    private long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            return Long.parseLong(new String(Base64.getDecoder().decode(cursor)));
        } catch (Exception e) {
            log.warn("Cursor 解码失败：{}，重置为首页", cursor);
            return 0;
        }
    }

    private String generateOrderNo() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "ORD-" + year + "-";
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

    private Map<Long, String> batchLoadUserNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> result = new HashMap<>();
        users.forEach(u -> result.put(u.getId(), u.getName()));
        return result;
    }

    /** 批量查订单任务统计：orderId -> [taskCount, approvedCount] */
    private Map<Long, int[]> batchLoadTaskStats(List<Long> orderIds) {
        Map<Long, int[]> result = new HashMap<>();
        if (orderIds == null || orderIds.isEmpty()) return result;

        List<OrderTask> allTasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>().in(OrderTask::getOrderId, orderIds)
        );

        for (OrderTask t : allTasks) {
            int[] stats = result.computeIfAbsent(t.getOrderId(), k -> new int[]{0, 0});
            stats[0]++;
            if (OrderStatus.APPROVED.getCode().equals(t.getStatus())) {
                stats[1]++;
            }
        }
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

        vo.setReferenceFiles(refFiles.stream().map(r -> {
            ReferenceFileVO rf = new ReferenceFileVO();
            rf.setId(r.getId());
            rf.setFileUrl(r.getFileUrl());
            rf.setFileName(r.getFileName());
            rf.setFileSize(r.getFileSize());
            return rf;
        }).toList());

        vo.setDesignVersions(versions.stream().map(v -> {
            DesignVersionVO dv = new DesignVersionVO();
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

        Map<Long, String> versionNoMap = versions.stream()
                .collect(Collectors.toMap(DesignVersion::getId, DesignVersion::getVersionNo));
        vo.setFeedbacks(feedbacks.stream().map(f -> {
            OrderFeedbackVO fb = new OrderFeedbackVO();
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
