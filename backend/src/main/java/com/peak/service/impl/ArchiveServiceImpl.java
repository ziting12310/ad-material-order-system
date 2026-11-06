package com.peak.service.impl;

import com.peak.common.constants.ErrorCode;
import com.peak.entity.Order;
import com.peak.exception.BusinessException;
import com.peak.mapper.OrderMapper;
import com.peak.service.ArchiveService;
import com.peak.service.OrderService;
import com.peak.vo.CursorPage;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

/**
 * 归档查询服务 — 独立于定稿审核模块
 *
 * <p>口径：以订单（公司）为聚合单位，订单表 LEFT JOIN 物料表，按订单分组统计
 *  total / finishNum，全部物料定稿才算已归档，任意物料未定稿则为未归档；
 *  不做物料状态 WHERE 过滤，订单不会因无物料或未定稿而消失。
 * <p>设计师工作台的物料级状态展示不受本模块影响。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    // ========================================================================
    // 【1】归档查询：订单列表（全部订单，含已归档/未归档）
    // ========================================================================
    @Override
    public CursorPage<OrderListItemVO> listArchivedOrders(Long userId, String userRole,
                                                           String cursor, int limit,
                                                           String materialType, String keyword,
                                                           String deliveredDateFrom, String deliveredDateTo,
                                                           String taskDeadlineFrom, String taskDeadlineTo) {
        long lastId = decodeCursor(cursor);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);

        LocalDate from = parseDate(deliveredDateFrom);
        LocalDate to = parseDate(deliveredDateTo);
        LocalDate dlFrom = parseDate(taskDeadlineFrom);
        LocalDate dlTo = parseDate(taskDeadlineTo);

        List<OrderListItemVO> all = orderMapper.selectArchivePage(
                userId, userRole,
                isBlank(materialType) ? null : materialType,
                isBlank(keyword) ? null : keyword,
                from, to, dlFrom, dlTo,
                lastId,
                effectiveLimit + 1);

        boolean hasMore = all.size() > effectiveLimit;
        List<OrderListItemVO> page = hasMore ? all.subList(0, effectiveLimit) : all;
        String nextCursor = hasMore ? encodeCursor(page.get(page.size() - 1).getId()) : null;

        return new CursorPage<>(page, nextCursor, hasMore);
    }

    private LocalDate parseDate(String date) {
        return isBlank(date) ? null : LocalDate.parse(date);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
    // 【3】归档公司数量 count — 与 listArchivedOrders 完全相同的筛选逻辑
    // 统计维度：按公司聚合，已归档订单的公司名称去重计数
    // ========================================================================
    @Override
    public long countArchived(Long userId, String userRole,
                              String materialType, String keyword,
                              String deliveredDateFrom, String deliveredDateTo,
                              String taskDeadlineFrom, String taskDeadlineTo) {
        // 与列表相同的 LEFT JOIN + 分组聚合口径，先判定已归档订单，再按公司去重计数
        return orderMapper.selectArchiveCount(
                userId, userRole,
                isBlank(materialType) ? null : materialType,
                isBlank(keyword) ? null : keyword,
                parseDate(deliveredDateFrom), parseDate(deliveredDateTo),
                parseDate(taskDeadlineFrom), parseDate(taskDeadlineTo));
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
