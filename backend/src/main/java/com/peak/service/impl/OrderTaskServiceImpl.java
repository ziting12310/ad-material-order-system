package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.FeedbackStatus;
import com.peak.common.enums.MaterialType;
import com.peak.common.enums.OperationType;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
import com.peak.dto.TaskAssignDTO;
import com.peak.dto.TaskCreateItem;
import com.peak.dto.TaskUpdateDTO;
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
import com.peak.service.OrderService;
import com.peak.service.OrderTaskService;
import com.peak.util.OperationLogUtil;
import com.peak.vo.OrderStatusUpdateResponseVO;
import com.peak.vo.OrderTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物料任务服务实现 — A 模块
 *
 * <p>职责：
 *  - 物料任务 CRUD（仅 CREATED 状态可编辑/删除）
 *  - 任务级状态流转：分派 / 接单 / 上传版本后推进 / 定稿 / 驳回
 *  - 任务级权限校验（设计师只能处理分派给自己的任务）
 *  - 操作日志记录
 *
 * <p>订单级状态由 OrderServiceImpl.aggregateOrderStatus 聚合所有任务状态得出，
 * 每次任务状态变化后调用 orderService.syncOrderStatus(orderId) 触发同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTaskServiceImpl implements OrderTaskService {

    private final OrderTaskMapper orderTaskMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final DesignVersionMapper designVersionMapper;
    private final OrderFeedbackMapper orderFeedbackMapper;
    private final OperationLogUtil opLog;
    private final OrderService orderService;

    // ========================================================================
    // 【1】批量创建任务（创建订单时调用，同事务，不记日志）
    // ========================================================================
    @Override
    public void batchCreateTasks(Long orderId, List<TaskCreateItem> items, Long creatorId) {
        Integer maxSort = orderTaskMapper.selectMaxSortOrderIncludingDeleted(orderId);
        int sort = (maxSort == null ? 0 : maxSort) + 1;
        for (TaskCreateItem item : items) {
            OrderTask task = new OrderTask();
            task.setOrderId(orderId);
            task.setTaskNo("T-" + String.format("%03d", sort));
            task.setMaterialType(item.getMaterialType());
            task.setQuantity(item.getQuantity());
            task.setSpecification(StringEscapeUtils.escapeHtml4(item.getSpecification()));
            task.setUsageDesc(item.getUsageDesc() != null ? StringEscapeUtils.escapeHtml4(item.getUsageDesc()) : null);
            task.setDeadline(item.getDeadline());
            task.setRemark(item.getRemark() != null ? StringEscapeUtils.escapeHtml4(item.getRemark()) : null);
            task.setStatus(OrderStatus.CREATED.getCode());
            task.setSortOrder(sort);
            task.setVersion(0);
            orderTaskMapper.insert(task);
            sort++;
        }
    }

    // ========================================================================
    // 【2】查询订单下所有任务
    // ========================================================================
    @Override
    public List<OrderTaskVO> listTasks(Long orderId, Long userId, String userRole) {
        // 校验订单存在 + 数据范围
        Order order = findOrderOrThrow(orderId);
        checkDataScope(order, userId, userRole);

        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>()
                        .eq(OrderTask::getOrderId, orderId)
                        .orderByAsc(OrderTask::getSortOrder)
                        .orderByAsc(OrderTask::getId)
        );

        // 批量查设计师姓名
        List<Long> designerIds = tasks.stream()
                .map(OrderTask::getDesignerId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNames = batchLoadUserNames(designerIds);

        // 批量查定稿版本号
        List<Long> finalVersionIds = tasks.stream()
                .map(OrderTask::getFinalVersionId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> versionNoMap = new HashMap<>();
        if (!finalVersionIds.isEmpty()) {
            List<DesignVersion> versions = designVersionMapper.selectBatchIds(finalVersionIds);
            for (DesignVersion v : versions) {
                versionNoMap.put(v.getId(), v.getVersionNo());
            }
        }

        return tasks.stream()
                .map(t -> buildTaskVO(t, userNames.get(t.getDesignerId()), versionNoMap.get(t.getFinalVersionId())))
                .collect(Collectors.toList());
    }

    // ========================================================================
    // 【3】查询单个任务详情
    // ========================================================================
    @Override
    public OrderTaskVO getTask(Long taskId, Long userId, String userRole) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());
        checkDataScope(order, userId, userRole);

        String designerName = null;
        if (task.getDesignerId() != null) {
            User u = userMapper.selectById(task.getDesignerId());
            designerName = u != null ? u.getName() : null;
        }

        String finalVersionNo = null;
        if (task.getFinalVersionId() != null) {
            DesignVersion v = designVersionMapper.selectById(task.getFinalVersionId());
            finalVersionNo = v != null ? v.getVersionNo() : null;
        }

        return buildTaskVO(task, designerName, finalVersionNo);
    }

    // ========================================================================
    // 【4】新增单个任务（已确认订单的变更，留操作日志）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderTaskVO addTask(Long orderId, TaskCreateItem item, Long operatorId) {
        Order order = findOrderOrThrow(orderId);

        // 权限：BL 或订单创建人（AM）
        String role = getUserRole(operatorId);
        if (!UserRole.BL.getCode().equals(role)
                && !(UserRole.AM.getCode().equals(role) && operatorId.equals(order.getCreatedBy()))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权新增物料任务");
        }

        // 已归档订单不能改
        checkNotArchived(order);

        // 计算下一个 sortOrder 和 taskNo（含已逻辑删除记录，避免撞唯一键）
        Integer maxSort = orderTaskMapper.selectMaxSortOrderIncludingDeleted(orderId);
        int nextSort = (maxSort == null ? 0 : maxSort) + 1;
        String taskNo = "T-" + String.format("%03d", nextSort);

        OrderTask task = new OrderTask();
        task.setOrderId(orderId);
        task.setTaskNo(taskNo);
        task.setMaterialType(item.getMaterialType());
        task.setQuantity(item.getQuantity());
        task.setSpecification(StringEscapeUtils.escapeHtml4(item.getSpecification()));
        task.setUsageDesc(item.getUsageDesc() != null ? StringEscapeUtils.escapeHtml4(item.getUsageDesc()) : null);
        task.setDeadline(item.getDeadline());
        task.setRemark(item.getRemark() != null ? StringEscapeUtils.escapeHtml4(item.getRemark()) : null);
        task.setStatus(OrderStatus.CREATED.getCode());
        task.setSortOrder(nextSort);
        task.setVersion(0);
        orderTaskMapper.insert(task);

        // 操作日志
        opLog.logTask(orderId, task.getId(), OperationType.TASK_ADD,
                "新增物料任务：" + getMaterialTypeName(item.getMaterialType()),
                operatorId, null, null);

        // 新增任务不触发订单状态聚合：避免已推进的订单状态被回退
        // 订单定稿 / 交付等关键动作仍会由 allTasksApproved 严格校验

        String designerName = task.getDesignerId() != null
                ? userMapper.selectById(task.getDesignerId()).getName() : null;
        return buildTaskVO(task, designerName, null);
    }

    // ========================================================================
    // 【5】修改任务（仅 CREATED 可修改）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderTaskVO updateTask(Long taskId, TaskUpdateDTO dto, Long operatorId) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());

        // 状态校验：仅 CREATED 可编辑
        if (!OrderStatus.CREATED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前任务状态为 [%s]，仅已登记状态可修改",
                    OrderStatus.of(task.getStatus()) != null ? OrderStatus.of(task.getStatus()).getLabel() : task.getStatus());
        }

        // 权限：BL 或订单创建人
        String role = getUserRole(operatorId);
        if (!UserRole.BL.getCode().equals(role)
                && !(UserRole.AM.getCode().equals(role) && operatorId.equals(order.getCreatedBy()))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权修改物料任务");
        }

        checkNotArchived(order);
        checkVersion(task, dto.getVersion());

        // 记录变更前
        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();

        if (dto.getMaterialType() != null && !dto.getMaterialType().equals(task.getMaterialType())) {
            before.append("物料类型:").append(task.getMaterialType()).append(";");
            task.setMaterialType(dto.getMaterialType());
            after.append("物料类型:").append(dto.getMaterialType()).append(";");
        }
        if (dto.getQuantity() != null && !dto.getQuantity().equals(task.getQuantity())) {
            before.append("数量:").append(task.getQuantity()).append(";");
            task.setQuantity(dto.getQuantity());
            after.append("数量:").append(dto.getQuantity()).append(";");
        }
        if (dto.getSpecification() != null && !dto.getSpecification().equals(task.getSpecification())) {
            before.append("规格:").append(task.getSpecification()).append(";");
            task.setSpecification(StringEscapeUtils.escapeHtml4(dto.getSpecification()));
            after.append("规格:").append(dto.getSpecification()).append(";");
        }
        if (dto.getUsageDesc() != null && !dto.getUsageDesc().equals(task.getUsageDesc())) {
            before.append("用途:").append(task.getUsageDesc()).append(";");
            task.setUsageDesc(StringEscapeUtils.escapeHtml4(dto.getUsageDesc()));
            after.append("用途:").append(dto.getUsageDesc()).append(";");
        }
        if (dto.getDeadline() != null && !dto.getDeadline().equals(task.getDeadline())) {
            before.append("截止:").append(task.getDeadline()).append(";");
            task.setDeadline(dto.getDeadline());
            after.append("截止:").append(dto.getDeadline()).append(";");
        }
        if (dto.getRemark() != null && !dto.getRemark().equals(task.getRemark())) {
            before.append("备注:").append(task.getRemark()).append(";");
            task.setRemark(StringEscapeUtils.escapeHtml4(dto.getRemark()));
            after.append("备注:").append(dto.getRemark()).append(";");
        }

        int rows = orderTaskMapper.updateById(task);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "任务已被他人修改，请刷新后重试");
        }

        // 操作日志（有变更才记）
        if (before.length() > 0) {
            opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_EDIT,
                    "修改物料任务" + task.getTaskNo(),
                    operatorId, before.toString(), after.toString());
        }

        String designerName = task.getDesignerId() != null
                ? userMapper.selectById(task.getDesignerId()).getName() : null;
        String finalVer = task.getFinalVersionId() != null
                ? designVersionMapper.selectById(task.getFinalVersionId()).getVersionNo() : null;
        return buildTaskVO(task, designerName, finalVer);
    }

    // ========================================================================
    // 【6】删除任务（仅 CREATED 可删除，至少保留一个）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTask(Long taskId, Long operatorId) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());

        if (!OrderStatus.CREATED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前任务状态为 [%s]，仅已登记状态可删除",
                    OrderStatus.of(task.getStatus()) != null ? OrderStatus.of(task.getStatus()).getLabel() : task.getStatus());
        }

        String role = getUserRole(operatorId);
        if (!UserRole.BL.getCode().equals(role)
                && !(UserRole.AM.getCode().equals(role) && operatorId.equals(order.getCreatedBy()))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权删除物料任务");
        }

        checkNotArchived(order);

        // 至少保留一个任务
        Long count = orderTaskMapper.selectCount(
                new LambdaQueryWrapper<OrderTask>().eq(OrderTask::getOrderId, task.getOrderId())
        );
        if (count <= 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "至少保留一个物料任务");
        }

        // 逻辑删除
        orderTaskMapper.deleteById(taskId);

        opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_REMOVE,
                "删除物料任务：" + task.getTaskNo() + "（" + getMaterialTypeName(task.getMaterialType()) + "）",
                operatorId, null, null);

        // 同步订单状态
        orderService.syncOrderStatus(task.getOrderId(), operatorId);
    }

    // ========================================================================
    // 【7】分派设计人员（CREATED → ASSIGNED）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderTaskVO assignTask(Long taskId, TaskAssignDTO dto, Long operatorId) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());

        if (!OrderStatus.CREATED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前任务状态为 [%s]，仅已登记状态可分派",
                    OrderStatus.of(task.getStatus()) != null ? OrderStatus.of(task.getStatus()).getLabel() : task.getStatus());
        }

        requireRole(operatorId, UserRole.BL.getCode());
        checkNotArchived(order);
        checkVersion(task, dto.getVersion());

        User designer = userMapper.selectById(dto.getDesignerId());
        if (designer == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "设计人员不存在");
        }
        if (!UserRole.DS.getCode().equals(designer.getRole())) {
            throw new BusinessException(ErrorCode.DESIGNER_ROLE_INVALID, "目标用户不是设计人员角色");
        }

        task.setStatus(OrderStatus.ASSIGNED.getCode());
        task.setDesignerId(dto.getDesignerId());
        task.setAssignedBy(operatorId);
        task.setAssignedAt(LocalDateTime.now());

        int rows = orderTaskMapper.updateById(task);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "任务已被他人修改，请刷新后重试");
        }

        opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_ASSIGN,
                "分派给设计人员：" + designer.getName(),
                operatorId, null, null);

        orderService.syncOrderStatus(task.getOrderId(), operatorId);

        return buildTaskVO(task, designer.getName(), null);
    }

    // ========================================================================
    // 【8】设计师接单（ASSIGNED → DESIGNING）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderStatusUpdateResponseVO acceptTask(Long taskId, Integer version, Long designerId) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());

        if (!OrderStatus.ASSIGNED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前任务状态为 [%s]，仅已分派状态可接单",
                    OrderStatus.of(task.getStatus()) != null ? OrderStatus.of(task.getStatus()).getLabel() : task.getStatus());
        }

        if (!designerId.equals(task.getDesignerId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "该任务未分派给您");
        }
        requireRole(designerId, UserRole.DS.getCode());

        checkNotArchived(order);
        checkVersion(task, version);

        task.setStatus(OrderStatus.DESIGNING.getCode());
        int rows = orderTaskMapper.updateById(task);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "任务已被他人修改，请刷新后重试");
        }

        opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_ACCEPT,
                "设计师接单",
                designerId, null, null);

        orderService.syncOrderStatus(task.getOrderId(), designerId);

        OrderStatusUpdateResponseVO resp = new OrderStatusUpdateResponseVO();
        resp.setId(task.getId());
        resp.setStatus(task.getStatus());
        resp.setVersion(task.getVersion());
        return resp;
    }

    // ========================================================================
    // 【9】上传版本后推进状态（DESIGNING → WAIT_APPROVE）
    // 由 DesignVersionServiceImpl 上传成功后回调
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onVersionUploaded(Long taskId, Long versionId, Long uploadedBy) {
        OrderTask task = orderTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "物料任务不存在");
        }
        Order order = findOrderOrThrow(task.getOrderId());
        checkNotArchived(order);

        // 只有 DESIGNING 状态才推进
        if (OrderStatus.DESIGNING.getCode().equals(task.getStatus())) {
            task.setStatus(OrderStatus.WAIT_APPROVE.getCode());
            int rows = orderTaskMapper.updateById(task);
            if (rows == 0) {
                throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                        "任务已被他人修改，请刷新后重试");
            }

            opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_UPLOAD_VERSION,
                    "上传设计版本",
                    uploadedBy, null, null);

            orderService.syncOrderStatus(task.getOrderId(), uploadedBy);
        }
    }

    // ========================================================================
    // 【9.1】客户提交意见后回退任务状态（WAIT_APPROVE → DESIGNING）
    // 由 OrderFeedbackServiceImpl 提交成功后回调，不在此自动创建意见
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onFeedbackSubmitted(Long taskId, Long feedbackId, Long operatorId) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());
        checkNotArchived(order);

        task.setStatus(OrderStatus.DESIGNING.getCode());
        int rows = orderTaskMapper.updateById(task);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "任务已被他人修改，请刷新后重试");
        }

        opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_FEEDBACK,
                "提交修改意见（反馈ID " + feedbackId + "）",
                operatorId, null, null);

        orderService.syncOrderStatus(task.getOrderId(), operatorId);
    }

    // ========================================================================
    // 【10】确认定稿（WAIT_APPROVE → APPROVED）— BL + AM 均可
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderTaskVO approveTask(Long taskId, String remark, Integer version, Long operatorId) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());

        if (!OrderStatus.WAIT_APPROVE.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前任务状态为 [%s]，仅待定稿状态可确认定稿",
                    OrderStatus.of(task.getStatus()) != null ? OrderStatus.of(task.getStatus()).getLabel() : task.getStatus());
        }

        // 权限：任务定稿仅业务负责人（BL）
        String role = getUserRole(operatorId);
        if (!UserRole.BL.getCode().equals(role)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "仅业务负责人可以确认任务定稿");
        }

        checkNotArchived(order);
        checkVersion(task, version);

        // 查最新版本，标记 final
        DesignVersion latest = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getTaskId, taskId)
                        .orderByDesc(DesignVersion::getId)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            throw new BusinessException(ErrorCode.DESIGN_VERSION_NOT_FOUND, "设计稿版本不存在");
        }
        // 同一任务只保留一个终稿，先清除旧的 final 标记
        designVersionMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DesignVersion>()
                        .eq(DesignVersion::getTaskId, taskId)
                        .eq(DesignVersion::getFinalVersion, 1)
                        .set(DesignVersion::getFinalVersion, 0));
        latest.setFinalVersion(1);
        int versionRows = designVersionMapper.updateById(latest);
        if (versionRows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "设计版本已被他人修改，请刷新后重试");
        }

        // 处理所有 PENDING Feedback
        LocalDateTime now = LocalDateTime.now();
        orderFeedbackMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderFeedback>()
                        .eq(OrderFeedback::getTaskId, taskId)
                        .eq(OrderFeedback::getStatus, FeedbackStatus.PENDING.getCode())
                        .set(OrderFeedback::getStatus, FeedbackStatus.PROCESSED.getCode())
                        .set(OrderFeedback::getProcessedAt, now)
        );

        task.setStatus(OrderStatus.APPROVED.getCode());
        task.setApprovedBy(operatorId);
        task.setApprovedAt(now);
        task.setFinalVersionId(latest.getId());
        task.setApproveRemark(remark != null ? StringEscapeUtils.escapeHtml4(remark) : null);

        int rows = orderTaskMapper.updateById(task);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "任务已被他人修改，请刷新后重试");
        }

        opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_APPROVE,
                "确认定稿（版本" + latest.getVersionNo() + "）",
                operatorId, null, null);

        orderService.syncOrderStatus(task.getOrderId(), operatorId);

        String approverName = userMapper.selectById(operatorId).getName();
        OrderTaskVO vo = buildTaskVO(task,
                task.getDesignerId() != null ? userMapper.selectById(task.getDesignerId()).getName() : null,
                latest.getVersionNo());
        vo.setApprovedByName(approverName);
        return vo;
    }

    // ========================================================================
    // 【11】驳回定稿（WAIT_APPROVE → DESIGNING + 自动 feedback）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderTaskVO rejectTask(Long taskId, String rejectNote, Integer version, Long operatorId) {
        OrderTask task = findTaskOrThrow(taskId);
        Order order = findOrderOrThrow(task.getOrderId());

        if (!OrderStatus.WAIT_APPROVE.getCode().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前任务状态为 [%s]，仅待定稿状态可驳回",
                    OrderStatus.of(task.getStatus()) != null ? OrderStatus.of(task.getStatus()).getLabel() : task.getStatus());
        }

        String role = getUserRole(operatorId);
        if (!UserRole.BL.getCode().equals(role)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "仅业务负责人可以驳回任务");
        }

        checkNotArchived(order);
        checkVersion(task, version);

        task.setStatus(OrderStatus.DESIGNING.getCode());
        int rows = orderTaskMapper.updateById(task);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "任务已被他人修改，请刷新后重试");
        }

        // 自动创建 Feedback
        // 找最新版本
        DesignVersion latest = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getTaskId, taskId)
                        .orderByDesc(DesignVersion::getId)
                        .last("LIMIT 1")
        );

        OrderFeedback feedback = new OrderFeedback();
        feedback.setOrderId(task.getOrderId());
        feedback.setTaskId(taskId);
        feedback.setVersionId(latest != null ? latest.getId() : null);
        feedback.setContent(StringEscapeUtils.escapeHtml4(rejectNote));
        feedback.setStatus(FeedbackStatus.PENDING.getCode());
        feedback.setSubmittedBy(operatorId);
        feedback.setSubmittedAt(LocalDateTime.now());
        orderFeedbackMapper.insert(feedback);

        opLog.logTask(task.getOrderId(), taskId, OperationType.TASK_REJECT,
                "驳回定稿：" + rejectNote,
                operatorId, null, null);

        orderService.syncOrderStatus(task.getOrderId(), operatorId);

        String designerName = task.getDesignerId() != null
                ? userMapper.selectById(task.getDesignerId()).getName() : null;
        return buildTaskVO(task, designerName, null);
    }

    // ========================================================================
    // 【12】检查订单下所有任务是否全部已定稿
    // ========================================================================
    @Override
    public boolean allTasksApproved(Long orderId) {
        List<OrderTask> tasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>().eq(OrderTask::getOrderId, orderId)
        );
        if (tasks.isEmpty()) return false;
        return tasks.stream()
                .allMatch(t -> OrderStatus.APPROVED.getCode().equals(t.getStatus())
                        || OrderStatus.DELIVERED.getCode().equals(t.getStatus()));
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    private OrderTask findTaskOrThrow(Long taskId) {
        OrderTask task = orderTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "物料任务不存在");
        }
        return task;
    }

    private Order findOrderOrThrow(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void checkVersion(OrderTask task, Integer requestVersion) {
        if (!task.getVersion().equals(requestVersion)) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "数据已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    task.getVersion(), requestVersion);
        }
    }

    private void requireRole(Long userId, String requiredRole) {
        String actualRole = getUserRole(userId);
        if (!requiredRole.equals(actualRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "需要 %s 角色才能执行该操作", UserRole.of(requiredRole).getLabel());
        }
    }

    private String getUserRole(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null ? u.getRole() : null;
    }

    private void checkDataScope(Order order, Long userId, String role) {
        if (UserRole.AM.getCode().equals(role)) {
            if (!userId.equals(order.getCreatedBy())) {
                throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看他人订单");
            }
        } else if (UserRole.DS.getCode().equals(role)) {
            // DS 看订单下是否有分派给自己的任务
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

    private void checkNotArchived(Order order) {
        if (Integer.valueOf(1).equals(order.getArchived())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "订单已归档，不可修改");
        }
    }

    private Map<Long, String> batchLoadUserNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> result = new HashMap<>();
        users.forEach(u -> result.put(u.getId(), u.getName()));
        return result;
    }

    private String getMaterialTypeName(String code) {
        MaterialType t = MaterialType.of(code);
        return t != null ? t.getLabel() : code;
    }

    private OrderTaskVO buildTaskVO(OrderTask task, String designerName, String finalVersionNo) {
        OrderTaskVO vo = new OrderTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setMaterialType(task.getMaterialType());
        vo.setMaterialTypeName(getMaterialTypeName(task.getMaterialType()));
        vo.setQuantity(task.getQuantity());
        vo.setSpecification(task.getSpecification());
        vo.setUsageDesc(task.getUsageDesc());
        vo.setDeadline(task.getDeadline());
        vo.setRemark(task.getRemark());
        vo.setStatus(task.getStatus());
        OrderStatus s = OrderStatus.of(task.getStatus());
        vo.setStatusName(s != null ? s.getLabel() : task.getStatus());
        vo.setDesignerId(task.getDesignerId());
        vo.setDesignerName(designerName);
        vo.setApprovedAt(task.getApprovedAt());
        vo.setFinalVersionNo(finalVersionNo);
        vo.setSortOrder(task.getSortOrder());
        vo.setVersion(task.getVersion());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }
}
