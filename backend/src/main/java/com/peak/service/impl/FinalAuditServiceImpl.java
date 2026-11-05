package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.MaterialType;
import com.peak.common.enums.OperationType;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
import com.peak.entity.Order;
import com.peak.entity.OrderOperationLog;
import com.peak.entity.OrderTask;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.OrderOperationLogMapper;
import com.peak.mapper.OrderTaskMapper;
import com.peak.mapper.UserMapper;
import com.peak.service.FinalAuditService;
import com.peak.vo.AuditStatsVO;
import com.peak.vo.CursorPage;
import com.peak.vo.OperationLogVO;
import com.peak.vo.OrderTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定稿审核服务实现 — C 模块（审核域）
 *
 * <p>专注审核视角的查询，不重复 A 模块的写操作。
 * 定稿 / 驳回的写操作由 OrderTaskService（任务级）和 OrderService（订单级）处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinalAuditServiceImpl implements FinalAuditService {

    private final OrderTaskMapper orderTaskMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final OrderOperationLogMapper operationLogMapper;

    // ========================================================================
    // 【1】待审核任务列表（我的待办）
    // ========================================================================
    @Override
    public CursorPage<OrderTaskVO> listPendingTasks(Long userId, String userRole,
                                                     String cursor, int limit,
                                                     String materialType, String keyword) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<OrderTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(OrderTask::getId, lastId);
        wrapper.eq(OrderTask::getStatus, OrderStatus.WAIT_APPROVE.getCode());
        wrapper.orderByDesc(OrderTask::getUpdatedAt);
        wrapper.orderByDesc(OrderTask::getId);
        wrapper.last("LIMIT " + (effectiveLimit + 1));

        // 数据范围
        if (UserRole.AM.getCode().equals(userRole)) {
            // AM：只能看自己创建的订单下的待审核任务
            List<Long> myOrderIds = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getCreatedBy, userId)
                            .select(Order::getId)
            ).stream().map(Order::getId).collect(Collectors.toList());

            if (myOrderIds.isEmpty()) {
                return new CursorPage<>(Collections.emptyList(), null, false);
            }
            wrapper.in(OrderTask::getOrderId, myOrderIds);
        } else if (UserRole.DS.getCode().equals(userRole)) {
            // DS：看不到待审核列表（不是审核角色）
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "设计人员无审核权限");
        }
        // BL：看全部

        // 过滤条件
        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(OrderTask::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            // 按任务编号或规格模糊搜
            wrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }

        List<OrderTask> all = orderTaskMapper.selectList(wrapper);
        boolean hasMore = all.size() > effectiveLimit;
        List<OrderTask> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        // 批量查设计师姓名 + 订单号
        Map<Long, String> designerNames = batchLoadUserNames(
                page.stream().map(OrderTask::getDesignerId).filter(id -> id != null).distinct().toList());

        List<Long> orderIds = page.stream().map(OrderTask::getOrderId).distinct().toList();
        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<Order> orders = orderMapper.selectBatchIds(orderIds);
            for (Order o : orders) {
                orderNoMap.put(o.getId(), o.getOrderNo());
            }
        }

        List<OrderTaskVO> voList = page.stream().map(t -> {
            OrderTaskVO vo = new OrderTaskVO();
            vo.setId(t.getId());
            vo.setTaskNo(t.getTaskNo());
            vo.setMaterialType(t.getMaterialType());
            MaterialType mt = MaterialType.of(t.getMaterialType());
            vo.setMaterialTypeName(mt != null ? mt.getLabel() : t.getMaterialType());
            vo.setQuantity(t.getQuantity());
            vo.setSpecification(t.getSpecification());
            vo.setUsageDesc(t.getUsageDesc());
            vo.setDeadline(t.getDeadline());
            vo.setStatus(t.getStatus());
            OrderStatus s = OrderStatus.of(t.getStatus());
            vo.setStatusName(s != null ? s.getLabel() : t.getStatus());
            vo.setDesignerId(t.getDesignerId());
            vo.setDesignerName(designerNames.get(t.getDesignerId()));
            vo.setSortOrder(t.getSortOrder());
            vo.setVersion(t.getVersion());
            vo.setCreatedAt(t.getCreatedAt());
            return vo;
        }).toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    @Override
    public long countPendingTasks(Long userId, String userRole,
                                  String materialType, String keyword) {
        LambdaQueryWrapper<OrderTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderTask::getStatus, OrderStatus.WAIT_APPROVE.getCode());

        if (UserRole.AM.getCode().equals(userRole)) {
            List<Long> myOrderIds = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getCreatedBy, userId)
                            .select(Order::getId)
            ).stream().map(Order::getId).toList();
            if (myOrderIds.isEmpty()) {
                return 0L;
            }
            wrapper.in(OrderTask::getOrderId, myOrderIds);
        } else if (UserRole.DS.getCode().equals(userRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "设计人员无审核权限");
        }

        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(OrderTask::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            wrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }
        Long count = orderTaskMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    // ========================================================================
    // 【1.5】已定稿任务列表 & 数量（APPROVED / DELIVERED）
    // ========================================================================
    @Override
    public CursorPage<OrderTaskVO> listApprovedTasks(Long userId, String userRole,
                                                     String cursor, int limit,
                                                     String materialType, String keyword) {
        return buildTaskPage(userId, userRole, cursor, limit, materialType, keyword,
                List.of(OrderStatus.APPROVED.getCode(), OrderStatus.DELIVERED.getCode()));
    }

    @Override
    public long countApprovedTasks(Long userId, String userRole,
                                   String materialType, String keyword) {
        return countByStatus(userId, userRole, materialType, keyword,
                List.of(OrderStatus.APPROVED.getCode(), OrderStatus.DELIVERED.getCode()));
    }

    // ========================================================================
    // 【1.6】已驳回任务列表 & 数量（最近一次审核操作是 TASK_REJECT，后续无 TASK_APPROVE）
    // ========================================================================
    @Override
    public CursorPage<OrderTaskVO> listRejectedTasks(Long userId, String userRole,
                                                      String cursor, int limit,
                                                      String materialType, String keyword) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        // 数据范围 scopeOrderIds（与 listPendingTasks 完全一致）
        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);

        // 第一步：查出所有审核相关的 OperationLog（TASK_APPROVE / TASK_REJECT）
        LambdaQueryWrapper<OrderOperationLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.in(OrderOperationLog::getOperationType,
                OperationType.TASK_APPROVE.getCode(), OperationType.TASK_REJECT.getCode());
        if (scopeOrderIds != null) {
            logWrapper.in(OrderOperationLog::getOrderId, scopeOrderIds);
        }
        logWrapper.orderByDesc(OrderOperationLog::getOperatedAt);
        logWrapper.orderByDesc(OrderOperationLog::getId);

        List<OrderOperationLog> allAuditLogs = operationLogMapper.selectList(logWrapper);
        if (allAuditLogs.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), null, false);
        }

        // 第二步：按 taskId 分组，取最近一条 → 过滤出"最近一次是 TASK_REJECT"的 taskIds
        // 同时记录每个 taskId 对应的 reject 时间用于排序
        Map<Long, java.time.LocalDateTime> taskRejectTime = new java.util.LinkedHashMap<>();
        Set<Long> approvedRecentlyTaskIds = new java.util.HashSet<>();
        for (OrderOperationLog log : allAuditLogs) {
            Long tid = log.getTaskId();
            if (tid == null) continue;
            if (OperationType.TASK_APPROVE.getCode().equals(log.getOperationType())) {
                approvedRecentlyTaskIds.add(tid);
            } else if (OperationType.TASK_REJECT.getCode().equals(log.getOperationType())) {
                if (!approvedRecentlyTaskIds.contains(tid) && !taskRejectTime.containsKey(tid)) {
                    taskRejectTime.put(tid, log.getOperatedAt());
                }
            }
        }

        // 注意：taskRejectTime 的插入顺序就是按 operatedAt DESC 排好的（LinkedHashMap）
        List<Long> rejectedTaskIds = new ArrayList<>(taskRejectTime.keySet());
        if (rejectedTaskIds.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), null, false);
        }

        // 第三步：查这些任务的实体，追加过滤条件（materialType / keyword）
        // 但 rejectedTaskIds 已按时间 DESC，需要用 IN 查询后在内存过滤
        LambdaQueryWrapper<OrderTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(OrderTask::getId, rejectedTaskIds);
        if (materialType != null && !materialType.isBlank()) {
            taskWrapper.eq(OrderTask::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            taskWrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }
        List<OrderTask> allMatched = orderTaskMapper.selectList(taskWrapper);

        // 第四步：按 rejectedTaskIds 的顺序（即 operatedAt DESC）排序 + 游标分页
        Map<Long, OrderTask> taskMap = new HashMap<>();
        allMatched.forEach(t -> taskMap.put(t.getId(), t));
        List<OrderTask> sorted = new ArrayList<>();
        for (Long tid : rejectedTaskIds) {
            OrderTask t = taskMap.get(tid);
            if (t != null) sorted.add(t);
        }

        // 游标分页：用任务的 id 游标（与 listPendingTasks 一致）
        List<OrderTask> afterCursor = new ArrayList<>();
        boolean passedCursor = lastId == 0;
        for (OrderTask t : sorted) {
            if (passedCursor && t.getId() > lastId) {
                afterCursor.add(t);
            }
            if (!passedCursor && t.getId() == lastId) {
                passedCursor = true;
            }
        }
        if (!passedCursor && lastId != 0) {
            // lastId 不在列表中，说明是过期游标，从头开始
            afterCursor = new ArrayList<>(sorted);
        }

        boolean hasMore = afterCursor.size() > effectiveLimit;
        List<OrderTask> page = hasMore ? afterCursor.subList(0, effectiveLimit) : afterCursor;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        if (page.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), null, false);
        }

        // 组装 VO（复用 listPendingTasks 的逻辑）
        Map<Long, String> designerNames = batchLoadUserNames(
                page.stream().map(OrderTask::getDesignerId).filter(id -> id != null).distinct().toList());
        List<Long> orderIds = page.stream().map(OrderTask::getOrderId).distinct().toList();
        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(o -> orderNoMap.put(o.getId(), o.getOrderNo()));
        }

        List<OrderTaskVO> voList = page.stream().map(t -> {
            OrderTaskVO vo = new OrderTaskVO();
            vo.setId(t.getId());
            vo.setTaskNo(t.getTaskNo());
            vo.setMaterialType(t.getMaterialType());
            MaterialType mt = MaterialType.of(t.getMaterialType());
            vo.setMaterialTypeName(mt != null ? mt.getLabel() : t.getMaterialType());
            vo.setQuantity(t.getQuantity());
            vo.setSpecification(t.getSpecification());
            vo.setUsageDesc(t.getUsageDesc());
            vo.setDeadline(t.getDeadline());
            vo.setStatus(t.getStatus());
            OrderStatus s = OrderStatus.of(t.getStatus());
            vo.setStatusName(s != null ? s.getLabel() : t.getStatus());
            vo.setDesignerId(t.getDesignerId());
            vo.setDesignerName(designerNames.get(t.getDesignerId()));
            vo.setSortOrder(t.getSortOrder());
            vo.setVersion(t.getVersion());
            vo.setCreatedAt(t.getCreatedAt());
            return vo;
        }).toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    @Override
    public long countRejectedTasks(Long userId, String userRole,
                                   String materialType, String keyword) {
        // 复用 listRejectedTasks 中前 3 步的逻辑（不做游标分页），最后 selectCount
        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);

        LambdaQueryWrapper<OrderOperationLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.in(OrderOperationLog::getOperationType,
                OperationType.TASK_APPROVE.getCode(), OperationType.TASK_REJECT.getCode());
        if (scopeOrderIds != null) {
            logWrapper.in(OrderOperationLog::getOrderId, scopeOrderIds);
        }
        logWrapper.orderByDesc(OrderOperationLog::getOperatedAt);
        logWrapper.orderByDesc(OrderOperationLog::getId);

        List<OrderOperationLog> logs = operationLogMapper.selectList(logWrapper);
        if (logs.isEmpty()) return 0L;

        Set<Long> rejectedTaskIds = new java.util.HashSet<>();
        Set<Long> approvedRecentlyTaskIds = new java.util.HashSet<>();
        for (OrderOperationLog log : logs) {
            Long tid = log.getTaskId();
            if (tid == null) continue;
            if (OperationType.TASK_APPROVE.getCode().equals(log.getOperationType())) {
                approvedRecentlyTaskIds.add(tid);
            } else if (OperationType.TASK_REJECT.getCode().equals(log.getOperationType())) {
                if (!approvedRecentlyTaskIds.contains(tid)) {
                    rejectedTaskIds.add(tid);
                }
            }
        }
        if (rejectedTaskIds.isEmpty()) return 0L;

        LambdaQueryWrapper<OrderTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(OrderTask::getId, rejectedTaskIds);
        if (materialType != null && !materialType.isBlank()) {
            taskWrapper.eq(OrderTask::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            taskWrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }
        Long count = orderTaskMapper.selectCount(taskWrapper);
        return count != null ? count : 0L;
    }

    // ========================================================================
    // 【辅助方法】复用的通用逻辑
    // ========================================================================
    /** 按状态集合查询任务列表（复用 listPendingTasks 骨架） */
    private CursorPage<OrderTaskVO> buildTaskPage(Long userId, String userRole,
                                                   String cursor, int limit,
                                                   String materialType, String keyword,
                                                   List<String> statusList) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<OrderTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(OrderTask::getId, lastId);
        wrapper.in(OrderTask::getStatus, statusList);
        wrapper.orderByDesc(OrderTask::getUpdatedAt);
        wrapper.orderByDesc(OrderTask::getId);
        wrapper.last("LIMIT " + (effectiveLimit + 1));

        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);
        if (scopeOrderIds != null) {
            wrapper.in(OrderTask::getOrderId, scopeOrderIds);
        }

        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(OrderTask::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            wrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }

        List<OrderTask> all = orderTaskMapper.selectList(wrapper);
        boolean hasMore = all.size() > effectiveLimit;
        List<OrderTask> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        Map<Long, String> designerNames = batchLoadUserNames(
                page.stream().map(OrderTask::getDesignerId).filter(id -> id != null).distinct().toList());
        List<Long> orderIds = page.stream().map(OrderTask::getOrderId).distinct().toList();
        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(o -> orderNoMap.put(o.getId(), o.getOrderNo()));
        }

        List<OrderTaskVO> voList = page.stream().map(t -> {
            OrderTaskVO vo = new OrderTaskVO();
            vo.setId(t.getId());
            vo.setTaskNo(t.getTaskNo());
            vo.setMaterialType(t.getMaterialType());
            MaterialType mt = MaterialType.of(t.getMaterialType());
            vo.setMaterialTypeName(mt != null ? mt.getLabel() : t.getMaterialType());
            vo.setQuantity(t.getQuantity());
            vo.setSpecification(t.getSpecification());
            vo.setUsageDesc(t.getUsageDesc());
            vo.setDeadline(t.getDeadline());
            vo.setStatus(t.getStatus());
            OrderStatus s = OrderStatus.of(t.getStatus());
            vo.setStatusName(s != null ? s.getLabel() : t.getStatus());
            vo.setDesignerId(t.getDesignerId());
            vo.setDesignerName(designerNames.get(t.getDesignerId()));
            vo.setSortOrder(t.getSortOrder());
            vo.setVersion(t.getVersion());
            vo.setCreatedAt(t.getCreatedAt());
            return vo;
        }).toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    /** 按状态集合查询任务数量 */
    private long countByStatus(Long userId, String userRole,
                               String materialType, String keyword,
                               List<String> statusList) {
        LambdaQueryWrapper<OrderTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderTask::getStatus, statusList);

        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);
        if (scopeOrderIds != null) {
            wrapper.in(OrderTask::getOrderId, scopeOrderIds);
        }

        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(OrderTask::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            wrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }
        Long count = orderTaskMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /** 解析当前用户的数据范围 — 返回 null 表示无限制（BL），空 list 表示 AM 没创建过订单 */
    private List<Long> resolveScopeOrderIds(Long userId, String userRole) {
        if (UserRole.DS.getCode().equals(userRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "设计人员无审核权限");
        }
        if (UserRole.AM.getCode().equals(userRole)) {
            List<Long> ids = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getCreatedBy, userId)
                            .select(Order::getId)
            ).stream().map(Order::getId).toList();
            return ids.isEmpty() ? Collections.emptyList() : ids;
        }
        // BL → null 表示不限制
        return null;
    }

    @Override
    public AuditStatsVO getAuditStats(Long userId, String userRole) {
        AuditStatsVO stats = new AuditStatsVO();

        List<Long> scopeOrderIds = null;
        if (UserRole.AM.getCode().equals(userRole)) {
            scopeOrderIds = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getCreatedBy, userId)
                            .select(Order::getId)
            ).stream().map(Order::getId).toList();
            if (scopeOrderIds.isEmpty()) {
                return stats;
            }
        } else if (UserRole.DS.getCode().equals(userRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "设计人员无审核权限");
        }

        // 待审核
        LambdaQueryWrapper<OrderTask> pending = new LambdaQueryWrapper<>();
        pending.eq(OrderTask::getStatus, OrderStatus.WAIT_APPROVE.getCode());
        if (scopeOrderIds != null) {
            pending.in(OrderTask::getOrderId, scopeOrderIds);
        }
        stats.setPendingCount(nullSafe(orderTaskMapper.selectCount(pending)));

        // 已定稿
        LambdaQueryWrapper<OrderTask> approved = new LambdaQueryWrapper<>();
        approved.in(OrderTask::getStatus,
                OrderStatus.APPROVED.getCode(), OrderStatus.DELIVERED.getCode());
        if (scopeOrderIds != null) {
            approved.in(OrderTask::getOrderId, scopeOrderIds);
        }
        stats.setApprovedCount(nullSafe(orderTaskMapper.selectCount(approved)));

        // 驳回次数（按审核操作日志统计）
        LambdaQueryWrapper<OrderOperationLog> rejected = new LambdaQueryWrapper<>();
        rejected.in(OrderOperationLog::getOperationType,
                OperationType.TASK_REJECT.getCode(), OperationType.ORDER_REJECT.getCode());
        if (scopeOrderIds != null) {
            rejected.in(OrderOperationLog::getOrderId, scopeOrderIds);
        }
        stats.setRejectedCount(nullSafe(operationLogMapper.selectCount(rejected)));

        return stats;
    }

    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }

    // ========================================================================
    // 【2】我参与的审核记录
    // ========================================================================
    @Override
    public List<OperationLogVO> listMyAuditLogs(Long userId, String userRole, int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        // 审核相关操作类型
        List<String> auditTypes = List.of(
                OperationType.TASK_APPROVE.getCode(),
                OperationType.TASK_REJECT.getCode(),
                OperationType.ORDER_CONFIRM.getCode(),
                OperationType.ORDER_DELIVER.getCode()
        );

        List<OrderOperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OrderOperationLog>()
                        .eq(OrderOperationLog::getOperatorId, userId)
                        .in(OrderOperationLog::getOperationType, auditTypes)
                        .orderByDesc(OrderOperationLog::getOperatedAt)
                        .orderByDesc(OrderOperationLog::getId)
                        .last("LIMIT " + effectiveLimit)
        );

        // 查 taskNo
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

        return logs.stream().map(l -> buildLogVO(l, taskNoMap)).toList();
    }

    // ========================================================================
    // 【3】按订单查审核记录
    // ========================================================================
    @Override
    public List<OperationLogVO> listAuditLogsByOrder(Long orderId, Long userId, String userRole) {
        // 校验订单存在 + 数据范围
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (UserRole.AM.getCode().equals(userRole) && !userId.equals(order.getCreatedBy())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看他人订单");
        }
        if (UserRole.DS.getCode().equals(userRole)) {
            Long assigned = orderTaskMapper.selectCount(
                    new LambdaQueryWrapper<OrderTask>()
                            .eq(OrderTask::getOrderId, orderId)
                            .eq(OrderTask::getDesignerId, userId));
            if (assigned == null || assigned == 0) {
                throw new BusinessException(ErrorCode.DATA_SCOPE_DENIED, "无权查看未分派给自己的订单日志");
            }
        }

        List<String> auditTypes = List.of(
                OperationType.TASK_APPROVE.getCode(),
                OperationType.TASK_REJECT.getCode(),
                OperationType.ORDER_CONFIRM.getCode(),
                OperationType.ORDER_DELIVER.getCode()
        );

        List<OrderOperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OrderOperationLog>()
                        .eq(OrderOperationLog::getOrderId, orderId)
                        .in(OrderOperationLog::getOperationType, auditTypes)
                        .orderByDesc(OrderOperationLog::getOperatedAt)
                        .orderByDesc(OrderOperationLog::getId)
        );

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

        return logs.stream().map(l -> buildLogVO(l, taskNoMap)).toList();
    }

    // ========================================================================
    //  私有辅助
    // ========================================================================

    private OperationLogVO buildLogVO(OrderOperationLog log, Map<Long, String> taskNoMap) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setTaskId(log.getTaskId());
        vo.setTaskNo(taskNoMap.get(log.getTaskId()));
        vo.setOperationType(log.getOperationType());
        OperationType ot = OperationType.of(log.getOperationType());
        vo.setOperationTypeName(ot != null ? ot.getLabel() : log.getOperationType());
        vo.setDescription(log.getDescription());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setOperatorRole(log.getOperatorRole());
        UserRole ur = UserRole.of(log.getOperatorRole());
        vo.setOperatorRoleName(ur != null ? ur.getLabel() : log.getOperatorRole());
        vo.setOperatedAt(log.getOperatedAt());
        return vo;
    }

    private Map<Long, String> batchLoadUserNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> result = new HashMap<>();
        users.forEach(u -> result.put(u.getId(), u.getName()));
        return result;
    }

    private String encodeCursor(long lastId) {
        return Base64.getEncoder().encodeToString(String.valueOf(lastId).getBytes());
    }

    private long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            return Long.parseLong(new String(Base64.getDecoder().decode(cursor)));
        } catch (Exception e) {
            log.warn("Cursor 解码失败：{}", cursor);
            return 0;
        }
    }
}
