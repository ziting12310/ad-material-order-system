package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.OrderStatus;
import com.peak.common.enums.UserRole;
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
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 归档服务实现 — C 模块（归档域）
 *
 * <p>所有归档查询都加了 archived=1 条件，确保只返回已归档数据。
 * 归档订单详情直接复用 OrderService.getOrderDetail，但额外校验归档状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final OrderMapper orderMapper;
    private final OrderTaskMapper orderTaskMapper;
    private final OrderService orderService;

    // ========================================================================
    // 【1】已归档订单列表
    // ========================================================================
    @Override
    public CursorPage<OrderListItemVO> listArchivedOrders(Long userId, String userRole,
                                                           String cursor, int limit,
                                                           String materialType, String keyword,
                                                           String deliveredDateFrom, String deliveredDateTo) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (lastId > 0) {
            wrapper.lt(Order::getId, lastId);
        }
        wrapper.eq(Order::getArchived, 1);
        wrapper.eq(Order::getStatus, OrderStatus.DELIVERED.getCode());
        wrapper.orderByDesc(Order::getId);
        wrapper.last("LIMIT " + (effectiveLimit + 1));

        // 数据范围
        applyDataScopeFilter(wrapper, userId, userRole);

        // 过滤
        if (materialType != null && !materialType.isBlank()) {
            wrapper.eq(Order::getMaterialType, materialType);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = StringEscapeUtils.escapeHtml4(keyword);
            wrapper.and(w -> w.like(Order::getOrderNo, safeKeyword)
                    .or().like(Order::getCustomerName, safeKeyword));
        }
        if (deliveredDateFrom != null && !deliveredDateFrom.isBlank()) {
            wrapper.ge(Order::getDeliveredAt, LocalDate.parse(deliveredDateFrom).atStartOfDay());
        }
        if (deliveredDateTo != null && !deliveredDateTo.isBlank()) {
            wrapper.le(Order::getDeliveredAt, LocalDate.parse(deliveredDateTo).atTime(23, 59, 59));
        }

        List<Order> all = orderMapper.selectList(wrapper);
        boolean hasMore = all.size() > effectiveLimit;
        List<Order> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        // 任务统计
        Map<Long, int[]> taskStats = batchLoadTaskStats(page.stream().map(Order::getId).toList());

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
            int[] stats = taskStats.getOrDefault(o.getId(), new int[]{0, 0});
            vo.setTaskCount(stats[0]);
            vo.setApprovedTaskCount(stats[1]);
            return vo;
        }).toList();

        return new CursorPage<>(voList, nextCursor, hasMore);
    }

    // ========================================================================
    // 【2】归档订单详情
    // ========================================================================
    @Override
    public OrderDetailVO getArchivedOrderDetail(Long orderId, Long userId, String userRole) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!Integer.valueOf(1).equals(order.getArchived())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "该订单尚未归档");
        }

        // 复用订单详情查询（已做数据范围校验）
        return orderService.getOrderDetail(orderId, userId, userRole);
    }

    // ========================================================================
    // 【3】归档订单数量统计
    // ========================================================================
    @Override
    public long countArchived(Long userId, String userRole) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getArchived, 1);
        wrapper.eq(Order::getStatus, OrderStatus.DELIVERED.getCode());
        applyDataScopeFilter(wrapper, userId, userRole);
        return orderMapper.selectCount(wrapper);
    }

    // ========================================================================
    //  私有辅助
    // ========================================================================

    private void applyDataScopeFilter(LambdaQueryWrapper<Order> wrapper, Long userId, String role) {
        if (UserRole.AM.getCode().equals(role)) {
            wrapper.eq(Order::getCreatedBy, userId);
        } else if (UserRole.DS.getCode().equals(role)) {
            // 多物料模型：DS 的可见范围按任务级 designer_id
            List<Long> orderIds = orderTaskMapper.selectList(
                    new LambdaQueryWrapper<OrderTask>()
                            .eq(OrderTask::getDesignerId, userId)
                            .select(OrderTask::getOrderId)
            ).stream().map(OrderTask::getOrderId).distinct().toList();
            if (orderIds.isEmpty()) {
                wrapper.eq(Order::getId, -1L);
            } else {
                wrapper.in(Order::getId, orderIds);
            }
        }
        // BL：全量
    }

    private Map<Long, int[]> batchLoadTaskStats(List<Long> orderIds) {
        Map<Long, int[]> result = new HashMap<>();
        if (orderIds == null || orderIds.isEmpty()) return result;

        List<OrderTask> allTasks = orderTaskMapper.selectList(
                new LambdaQueryWrapper<OrderTask>().in(OrderTask::getOrderId, orderIds)
        );

        for (OrderTask t : allTasks) {
            int[] stats = result.computeIfAbsent(t.getOrderId(), k -> new int[]{0, 0});
            stats[0]++;
            if (OrderStatus.APPROVED.getCode().equals(t.getStatus())
                    || OrderStatus.DELIVERED.getCode().equals(t.getStatus())) {
                stats[1]++;
            }
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
