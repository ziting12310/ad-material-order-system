package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
import com.peak.common.enums.MaterialType;
import com.peak.entity.Order;
import com.peak.entity.OrderTask;
import com.peak.exception.BusinessException;
import com.peak.mapper.OrderMapper;
import com.peak.mapper.OrderTaskMapper;
import com.peak.service.ArchiveService;
import com.peak.service.OrderService;
import com.peak.vo.CursorPage;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 归档查询服务 — 独立于定稿审核模块
 *
 * <p>数据来源：从已定稿（APPROVED）的物料数据向上关联订单/公司，
 *  按 orderId + customerName 维度聚合去重，保证所有已定稿订单都出现在归档列表。
 * <p>筛选条件（物料类型、关键字、交付日期）作用在底层已定稿物料上，聚合去重后输出。
 * <p>与定稿审核模块（FinalAuditService）解耦，不影响其物料粒度展示逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final OrderMapper orderMapper;
    private final OrderTaskMapper orderTaskMapper;
    private final OrderService orderService;

    // ========================================================================
    // 【1】归档查询：已定稿订单列表（按订单+公司聚合去重）
    // ========================================================================
    @Override
    public CursorPage<OrderListItemVO> listArchivedOrders(Long userId, String userRole,
                                                           String cursor, int limit,
                                                           String materialType, String keyword,
                                                           String deliveredDateFrom, String deliveredDateTo) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        // 第一步：从已定稿物料提取符合条件的 orderIds（聚合去重）
        Set<Long> matchedOrderIds = collectArchivedOrderIds(
                userId, userRole, materialType, keyword);

        if (matchedOrderIds.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), null, false);
        }

        // 第二步：查这些订单 + 交付日期区间 + 游标分页
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getId, matchedOrderIds);
        if (lastId > 0) {
            orderWrapper.lt(Order::getId, lastId); // 游标：向下翻
        }
        orderWrapper.orderByDesc(Order::getId);
        orderWrapper.last("LIMIT " + (effectiveLimit + 1));

        // 交付日期区间（作用在订单级 delivered_at）
        if (deliveredDateFrom != null && !deliveredDateFrom.isBlank()) {
            orderWrapper.ge(Order::getDeliveredAt, LocalDate.parse(deliveredDateFrom).atStartOfDay());
        }
        if (deliveredDateTo != null && !deliveredDateTo.isBlank()) {
            orderWrapper.le(Order::getDeliveredAt, LocalDate.parse(deliveredDateTo).atTime(23, 59, 59));
        }

        List<Order> all = orderMapper.selectList(orderWrapper);
        boolean hasMore = all.size() > effectiveLimit;
        List<Order> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        // 第三步：组装 VO（批量计算每个订单的已定稿任务数）
        Set<Long> pageOrderIds = new HashSet<>(page.stream().map(Order::getId).toList());
        Map<Long, Long> approvedTaskCountMap = countApprovedTasksByOrder(pageOrderIds);

        List<OrderListItemVO> voList = page.stream().map(o -> {
            OrderListItemVO vo = new OrderListItemVO();
            vo.setId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setCustomerName(o.getCustomerName());
            vo.setMaterialType(o.getMaterialType());
            vo.setQuantity(o.getQuantity());
            vo.setStatus(o.getStatus());
            vo.setDeliveryDate(o.getDeliveryDate());
            vo.setCreatedAt(o.getCreatedAt());
            // 物料数量：该订单已定稿的任务条数（聚合维度）
            vo.setTaskCount(approvedTaskCountMap.getOrDefault(o.getId(), 0L).intValue());
            return vo;
        }).toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    // ========================================================================
    // 【2】归档订单详情（只读，复用 OrderService）
    // ========================================================================
    @Override
    public OrderDetailVO getArchivedOrderDetail(Long orderId, Long userId, String userRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        // 复用订单详情查询（已做数据范围校验）
        return orderService.getOrderDetail(orderId, userId, userRole);
    }

    // ========================================================================
    // 【3】归档卡片 count — 与 listArchivedOrders 完全相同的筛选逻辑
    // ========================================================================
    @Override
    public long countArchived(Long userId, String userRole,
                              String materialType, String keyword,
                              String deliveredDateFrom, String deliveredDateTo) {
        // 复用 listArchivedOrders 的第一步：聚合去重后的订单集合
        Set<Long> matchedOrderIds = collectArchivedOrderIds(
                userId, userRole, materialType, keyword);

        if (matchedOrderIds.isEmpty()) return 0L;

        // 交付日期区间（作用在订单级，与 list 的第二步一致）
        if (deliveredDateFrom == null && deliveredDateTo == null) {
            return matchedOrderIds.size();
        }

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getId, matchedOrderIds);
        if (deliveredDateFrom != null && !deliveredDateFrom.isBlank()) {
            orderWrapper.ge(Order::getDeliveredAt, LocalDate.parse(deliveredDateFrom).atStartOfDay());
        }
        if (deliveredDateTo != null && !deliveredDateTo.isBlank()) {
            orderWrapper.le(Order::getDeliveredAt, LocalDate.parse(deliveredDateTo).atTime(23, 59, 59));
        }
        Long count = orderMapper.selectCount(orderWrapper);
        return count != null ? count : 0L;
    }

    // ========================================================================
    //  私有核心：从已定稿物料聚合订单（列表和 count 共用的查询条件对象）
    // ========================================================================

    /**
     * 从已定稿物料（APPROVED / DELIVERED）中，筛选出符合条件的 orderId 集合。
     * <p>筛选条件全部作用在底层物料（order_tasks）上：
     * <ul>
     *   <li>status IN (APPROVED, DELIVERED)</li>
     *   <li>materialType — 物料类型</li>
     *   <li>keyword — 任务编号/规格</li>
     *   <li>归属过滤 — BL 全量 / AM 限自己创建 / DS 限自己设计的任务</li>
     * </ul>
     * 返回 Set 天然去重 —— 同一个订单的多个已定稿物料只出现一次。
     */
    private Set<Long> collectArchivedOrderIds(Long userId, String userRole,
                                              String materialType, String keyword) {
        LambdaQueryWrapper<OrderTask> taskWrapper = new LambdaQueryWrapper<>();
        // 已定稿的物料（核心条件，覆盖 APPROVED + DELIVERED）
        taskWrapper.in(OrderTask::getStatus,
                OrderStatus.APPROVED.getCode(), OrderStatus.DELIVERED.getCode());

        // 归属过滤
        if (UserRole.AM.getCode().equals(userRole)) {
            // 客户经理：限自己创建的订单下的已定稿物料
            List<Long> ownOrderIds = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getCreatedBy, userId)
                            .select(Order::getId)
            ).stream().map(Order::getId).toList();
            if (ownOrderIds.isEmpty()) return Collections.emptySet();
            taskWrapper.in(OrderTask::getOrderId, ownOrderIds);
        } else if (UserRole.DS.getCode().equals(userRole)) {
            // 设计师：限自己参与设计的已定稿物料
            taskWrapper.eq(OrderTask::getDesignerId, userId);
        }
        // BL：全量，不加归属

        // 物料类型筛选
        if (materialType != null && !materialType.isBlank()) {
            taskWrapper.eq(OrderTask::getMaterialType, materialType);
        }
        // 关键字（底层任务编号/规格）
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            taskWrapper.and(w -> w.like(OrderTask::getTaskNo, safeKeyword)
                    .or().like(OrderTask::getSpecification, safeKeyword));
        }

        List<OrderTask> matchedTasks = orderTaskMapper.selectList(taskWrapper);
        Set<Long> orderIds = new HashSet<>();
        for (OrderTask t : matchedTasks) {
            if (t.getOrderId() != null) {
                orderIds.add(t.getOrderId());
            }
        }
        return orderIds;
    }

    /** 批量计算每个订单下已定稿（APPROVED/DELIVERED）的任务数量 */
    private Map<Long, Long> countApprovedTasksByOrder(Set<Long> orderIds) {
        Map<Long, Long> result = new HashMap<>();
        if (orderIds == null || orderIds.isEmpty()) return result;

        LambdaQueryWrapper<OrderTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderTask::getOrderId, orderIds);
        wrapper.in(OrderTask::getStatus,
                OrderStatus.APPROVED.getCode(), OrderStatus.DELIVERED.getCode());
        wrapper.select(OrderTask::getOrderId);

        List<OrderTask> tasks = orderTaskMapper.selectList(wrapper);
        for (OrderTask t : tasks) {
            result.merge(t.getOrderId(), 1L, Long::sum);
        }
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
