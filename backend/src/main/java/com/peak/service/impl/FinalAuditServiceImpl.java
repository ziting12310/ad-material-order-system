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
        Map<Long, String> customerNameMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(o -> {
                orderNoMap.put(o.getId(), o.getOrderNo());
                customerNameMap.put(o.getId(), o.getCustomerName());
            });
        }

        List<OrderTaskVO> voList = page.stream().map(t -> {
            OrderTaskVO vo = new OrderTaskVO();
            vo.setId(t.getId());
            vo.setOrderId(t.getOrderId());
            vo.setOrderNo(orderNoMap.get(t.getOrderId()));
            vo.setCustomerName(customerNameMap.get(t.getOrderId()));
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
    // 【1.5】已定稿任务列表 & 数量（共用同一套 queryWrapper，确保 count 与 list 严格对齐）
    // ========================================================================
    @Override
    public CursorPage<OrderTaskVO> listApprovedTasks(Long userId, String userRole,
                                                     String cursor, int limit,
                                                     String materialType, String keyword,
                                                     String deliveryDateFrom, String deliveryDateTo) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<OrderTask> wrapper = buildApprovedTaskWrapper(userId, userRole,
                materialType, keyword, deliveryDateFrom, deliveryDateTo);
        wrapper.gt(OrderTask::getId, lastId);
        wrapper.orderByDesc(OrderTask::getUpdatedAt);
        wrapper.orderByDesc(OrderTask::getId);
        wrapper.last("LIMIT " + (effectiveLimit + 1));

        List<OrderTask> all = orderTaskMapper.selectList(wrapper);
        boolean hasMore = all.size() > effectiveLimit;
        List<OrderTask> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        return buildTaskVOPage(page, nextCursor, hasMore);
    }

    @Override
    public long countApprovedTasks(Long userId, String userRole,
                                   String materialType, String keyword,
                                   String deliveryDateFrom, String deliveryDateTo) {
        // 与 listApprovedTasks 共用 buildApprovedTaskWrapper —— 完全相同的查询条件对象，仅去掉分页 limit
        LambdaQueryWrapper<OrderTask> wrapper = buildApprovedTaskWrapper(userId, userRole,
                materialType, keyword, deliveryDateFrom, deliveryDateTo);
        Long count = orderTaskMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /** 已定稿任务共享 queryWrapper — count 和 list 调用的是同一个对象，口径严格一致 */
    private LambdaQueryWrapper<OrderTask> buildApprovedTaskWrapper(Long userId, String userRole,
                                                                   String materialType, String keyword,
                                                                   String deliveryDateFrom, String deliveryDateTo) {
        LambdaQueryWrapper<OrderTask> wrapper = new LambdaQueryWrapper<>();

        // ① 状态：APPROVED + DELIVERED
        wrapper.in(OrderTask::getStatus,
                OrderStatus.APPROVED.getCode(), OrderStatus.DELIVERED.getCode());

        // ② 业务负责人归属过滤（BL 全量；AM 限自己创建的订单）
        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);
        if (scopeOrderIds != null) {
            wrapper.in(OrderTask::getOrderId, scopeOrderIds);
        }

        // ③ 排除作废订单下的任务
        wrapper.apply("order_id NOT IN (SELECT id FROM orders WHERE status = {0})",
                OrderStatus.CANCELLED.getCode());

        // ④ 筛选条件
        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(OrderTask::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            wrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }
        // ⑤ 交付日期区间（通过 OrderTask.deadline 字段）
        if (deliveryDateFrom != null && !deliveryDateFrom.isBlank()) {
            wrapper.ge(OrderTask::getDeadline, deliveryDateFrom);
        }
        if (deliveryDateTo != null && !deliveryDateTo.isBlank()) {
            wrapper.le(OrderTask::getDeadline, deliveryDateTo);
        }

        return wrapper;
    }

    // ========================================================================
    // 【1.6】已驳回：直接查 OperationLog 流水表（TASK_REJECT），不读任务主表状态
    // ========================================================================
    @Override
    public CursorPage<OrderTaskVO> listRejectedTasks(Long userId, String userRole,
                                                      String cursor, int limit,
                                                      String materialType, String keyword) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        // 1. 查 OperationLog：operationType=TASK_REJECT + 业务归属过滤
        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);
        LambdaQueryWrapper<OrderOperationLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(OrderOperationLog::getOperationType, OperationType.TASK_REJECT.getCode());
        logWrapper.gt(OrderOperationLog::getId, lastId);
        if (scopeOrderIds != null) {
            logWrapper.in(OrderOperationLog::getOrderId, scopeOrderIds);
        }
        logWrapper.orderByDesc(OrderOperationLog::getOperatedAt);
        logWrapper.orderByDesc(OrderOperationLog::getId);
        logWrapper.last("LIMIT " + (effectiveLimit + 1));

        List<OrderOperationLog> logs = operationLogMapper.selectList(logWrapper);
        boolean hasMore = logs.size() > effectiveLimit;
        List<OrderOperationLog> page = hasMore ? logs.subList(0, effectiveLimit) : logs;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        if (page.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), null, false);
        }

        // 2. 批量查关联 OrderTask / Order（用于 materialType/keyword 过滤 + VO 组装）
        List<Long> taskIds = page.stream().map(OrderOperationLog::getTaskId)
                .filter(id -> id != null).distinct().toList();
        List<Long> orderIds = page.stream().map(OrderOperationLog::getOrderId)
                .filter(id -> id != null).distinct().toList();

        Map<Long, OrderTask> taskMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            orderTaskMapper.selectBatchIds(taskIds).forEach(t -> taskMap.put(t.getId(), t));
        }
        Map<Long, Order> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(o -> orderMap.put(o.getId(), o));
        }

        // 3. 追加筛选（materialType/keyword 通过 taskMap 内存过滤）
        //    注意：logWrapper 只做归属+类型过滤，筛选条件在内存做
        List<OrderOperationLog> filtered = new ArrayList<>();
        for (OrderOperationLog log : page) {
            OrderTask task = taskMap.get(log.getTaskId());
            boolean match = true;
            if (task == null) {
                match = false;
            } else {
                if (materialType != null && !materialType.isBlank()
                        && !materialType.equals(task.getMaterialType())) {
                    match = false;
                }
                if (keyword != null && !keyword.isBlank()) {
                    String safe = StringEscapeUtils.escapeHtml4(keyword);
                    boolean hitTaskNo = task.getTaskNo() != null && task.getTaskNo().contains(safe);
                    boolean hitSpec = task.getSpecification() != null
                            && task.getSpecification().contains(safe);
                    Order order = orderMap.get(log.getOrderId());
                    boolean hitOrderNo = order != null && order.getOrderNo() != null
                            && order.getOrderNo().contains(safe);
                    if (!hitTaskNo && !hitSpec && !hitOrderNo) {
                        match = false;
                    }
                }
            }
            if (match) filtered.add(log);
        }

        // 4. 组装 VO
        Map<Long, String> designerNames = batchLoadUserNames(
                filtered.stream()
                        .map(l -> taskMap.get(l.getTaskId()))
                        .filter(t -> t != null && t.getDesignerId() != null)
                        .map(OrderTask::getDesignerId).distinct().toList());

        List<OrderTaskVO> voList = filtered.stream().map(log -> {
            OrderTaskVO vo = new OrderTaskVO();
            vo.setId(log.getTaskId());
            OrderTask task = taskMap.get(log.getTaskId());
            Order order = orderMap.get(log.getOrderId());
            if (task != null) {
                vo.setTaskNo(task.getTaskNo());
                vo.setMaterialType(task.getMaterialType());
                MaterialType mt = MaterialType.of(task.getMaterialType());
                vo.setMaterialTypeName(mt != null ? mt.getLabel() : task.getMaterialType());
                vo.setQuantity(task.getQuantity());
                vo.setSpecification(task.getSpecification());
                vo.setUsageDesc(task.getUsageDesc());
                vo.setDeadline(task.getDeadline());
                vo.setDesignerId(task.getDesignerId());
                vo.setDesignerName(task.getDesignerId() != null
                        ? designerNames.get(task.getDesignerId()) : null);
                vo.setSortOrder(task.getSortOrder());
                vo.setVersion(task.getVersion());
                vo.setCreatedAt(task.getCreatedAt());
            }
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setCustomerName(order.getCustomerName());
            }
            // 驳回任务的当前状态：DESIGNING（已退回设计中），但流水类型 REJECT 更明确
            vo.setStatus(OrderStatus.DESIGNING.getCode());
            vo.setStatusName("已驳回");
            return vo;
        }).toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    @Override
    public long countRejectedTasks(Long userId, String userRole,
                                   String materialType, String keyword) {
        // 用同一套逻辑：先查 TASK_REJECT 流水 + 归属过滤，再内存追加筛选
        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);
        LambdaQueryWrapper<OrderOperationLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(OrderOperationLog::getOperationType, OperationType.TASK_REJECT.getCode());
        if (scopeOrderIds != null) {
            logWrapper.in(OrderOperationLog::getOrderId, scopeOrderIds);
        }
        List<OrderOperationLog> allLogs = operationLogMapper.selectList(logWrapper);
        if (allLogs.isEmpty()) return 0L;

        // 批量查关联 OrderTask（用于筛选条件）
        List<Long> taskIds = allLogs.stream().map(OrderOperationLog::getTaskId)
                .filter(id -> id != null).distinct().toList();
        Map<Long, OrderTask> taskMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            orderTaskMapper.selectBatchIds(taskIds).forEach(t -> taskMap.put(t.getId(), t));
        }

        long count = 0;
        for (OrderOperationLog log : allLogs) {
            OrderTask task = taskMap.get(log.getTaskId());
            if (task == null) continue;
            if (materialType != null && !materialType.isBlank()
                    && !materialType.equals(task.getMaterialType())) continue;
            if (keyword != null && !keyword.isBlank()) {
                String safe = StringEscapeUtils.escapeHtml4(keyword);
                boolean hit = (task.getTaskNo() != null && task.getTaskNo().contains(safe))
                        || (task.getSpecification() != null && task.getSpecification().contains(safe));
                if (!hit) continue;
            }
            count++;
        }
        return count;
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

        return buildTaskVOPage(page, nextCursor, hasMore);
    }

    /** 将 OrderTask 分页实体组装为 OrderTaskVO（复用的通用逻辑） */
    private CursorPage<OrderTaskVO> buildTaskVOPage(List<OrderTask> page, String nextCursor, boolean hasMore) {
        if (page.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), nextCursor, false);
        }
        Map<Long, String> designerNames = batchLoadUserNames(
                page.stream().map(OrderTask::getDesignerId).filter(id -> id != null).distinct().toList());
        List<Long> orderIds = page.stream().map(OrderTask::getOrderId).distinct().toList();
        Map<Long, String> orderNoMap = new HashMap<>();
        Map<Long, String> customerNameMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(o -> {
                orderNoMap.put(o.getId(), o.getOrderNo());
                customerNameMap.put(o.getId(), o.getCustomerName());
            });
        }

        List<OrderTaskVO> voList = page.stream().map(t -> {
            OrderTaskVO vo = new OrderTaskVO();
            vo.setId(t.getId());
            vo.setOrderId(t.getOrderId());
            vo.setOrderNo(orderNoMap.get(t.getOrderId()));
            vo.setCustomerName(customerNameMap.get(t.getOrderId()));
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

        List<Long> scopeOrderIds = resolveScopeOrderIds(userId, userRole);

        // 待审核
        LambdaQueryWrapper<OrderTask> pending = new LambdaQueryWrapper<>();
        pending.eq(OrderTask::getStatus, OrderStatus.WAIT_APPROVE.getCode());
        if (scopeOrderIds != null) {
            pending.in(OrderTask::getOrderId, scopeOrderIds);
        }
        stats.setPendingCount(nullSafe(orderTaskMapper.selectCount(pending)));

        // 已定稿 — 与 countApprovedTasks 共用 buildApprovedTaskWrapper（排除 CANCELLED）
        stats.setApprovedCount(countApprovedTasks(userId, userRole, null, null, null, null));

        // 驳回 — 与 countRejectedTasks 对齐：只算 TASK_REJECT 流水（不算 ORDER_REJECT）
        long rejected = countRejectedTasks(userId, userRole, null, null);
        stats.setRejectedCount(rejected);

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

        // 查 taskNo + orderNo
        List<Long> taskIds = logs.stream()
                .map(OrderOperationLog::getTaskId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> taskNoMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            orderTaskMapper.selectBatchIds(taskIds).forEach(t -> taskNoMap.put(t.getId(), t.getTaskNo()));
        }
        // orderId 直接从 OperationLog 取，批量查 orders 得 orderNo
        List<Long> orderIds = logs.stream()
                .map(OrderOperationLog::getOrderId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(o -> orderNoMap.put(o.getId(), o.getOrderNo()));
        }

        return logs.stream().map(l -> buildLogVO(l, taskNoMap, orderNoMap)).toList();
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

        // 查 taskNo + orderNo
        List<Long> taskIds = logs.stream()
                .map(OrderOperationLog::getTaskId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> taskNoMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            orderTaskMapper.selectBatchIds(taskIds).forEach(t -> taskNoMap.put(t.getId(), t.getTaskNo()));
        }
        List<Long> orderIds = logs.stream()
                .map(OrderOperationLog::getOrderId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(o -> orderNoMap.put(o.getId(), o.getOrderNo()));
        }

        return logs.stream().map(l -> buildLogVO(l, taskNoMap, orderNoMap)).toList();
    }

    // ========================================================================
    //  私有辅助
    // ========================================================================

    private OperationLogVO buildLogVO(OrderOperationLog log,
                                       Map<Long, String> taskNoMap,
                                       Map<Long, String> orderNoMap) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setTaskId(log.getTaskId());
        vo.setTaskNo(taskNoMap.get(log.getTaskId()));
        vo.setOrderId(log.getOrderId());
        vo.setOrderNo(orderNoMap.get(log.getOrderId()));
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
