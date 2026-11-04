package com.peak.service;

import com.peak.vo.CursorPage;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderListItemVO;

/**
 * 归档服务接口 — C 模块（归档域）
 *
 * <p>提供归档视角的查询能力：
 *  - 已归档订单列表
 *  - 归档订单详情（只读）
 *  - 归档订单搜索
 *
 * <p>归档动作由 OrderService.markDelivered 触发（交付即归档）。
 */
public interface ArchiveService {

    /** 已归档订单列表（Cursor 分页） */
    CursorPage<OrderListItemVO> listArchivedOrders(Long userId, String userRole,
                                                    String cursor, int limit,
                                                    String materialType, String keyword,
                                                    String deliveredDateFrom, String deliveredDateTo);

    /** 归档订单详情（只读，和订单详情一致但明确标记已归档） */
    OrderDetailVO getArchivedOrderDetail(Long orderId, Long userId, String userRole);

    /** 归档订单数量统计（当前用户范围内） */
    long countArchived(Long userId, String userRole);
}
