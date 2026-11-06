package com.peak.service;

import com.peak.vo.CursorPage;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderListItemVO;

/**
 * 归档服务接口 — C 模块（归档域）
 */
public interface ArchiveService {

    /** 已归档订单列表（Cursor 分页） */
    CursorPage<OrderListItemVO> listArchivedOrders(Long userId, String userRole,
                                                    String cursor, int limit,
                                                    String materialType, String keyword,
                                                    String deliveredDateFrom, String deliveredDateTo,
                                                    String taskDeadlineFrom, String taskDeadlineTo);

    /** 归档订单详情（只读） */
    OrderDetailVO getArchivedOrderDetail(Long orderId, Long userId, String userRole);

    /** 归档订单数量统计 — 与 listArchivedOrders 完全相同的筛选逻辑 */
    long countArchived(Long userId, String userRole,
                       String materialType, String keyword,
                       String deliveredDateFrom, String deliveredDateTo,
                       String taskDeadlineFrom, String taskDeadlineTo);
}
