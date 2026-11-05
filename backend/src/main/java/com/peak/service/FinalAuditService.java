package com.peak.service;

import com.peak.vo.CursorPage;
import com.peak.vo.OperationLogVO;
import com.peak.vo.OrderTaskVO;

import java.util.List;

/**
 * 定稿审核服务接口 — C 模块（审核域）
 *
 * <p>提供审核视角的查询能力：
 *  - 待审核任务列表（我的待办）
 *  - 我参与的审核记录
 *  - 按条件查询审核历史
 */
public interface FinalAuditService {

    /**
     * 待审核任务列表（当前用户待办）
     *  - BL：看到所有 WAIT_APPROVE 状态的任务
     *  - AM：看到自己创建订单下 WAIT_APPROVE 状态的任务
     *  - DS：看不到
     */
    CursorPage<OrderTaskVO> listPendingTasks(Long userId, String userRole,
                                             String cursor, int limit,
                                             String materialType, String keyword);

    /** 待审核任务数量（与 listPendingTasks 同一数据范围与过滤条件） */
    long countPendingTasks(Long userId, String userRole,
                           String materialType, String keyword);

    /**
     * 已定稿任务列表（APPROVED / DELIVERED 状态）
     *  - BL：看到所有已定稿任务
     *  - AM：看到自己创建订单下已定稿任务
     *  - DS：看不到
     */
    CursorPage<OrderTaskVO> listApprovedTasks(Long userId, String userRole,
                                              String cursor, int limit,
                                              String materialType, String keyword);

    /** 已定稿任务数量（与 listApprovedTasks 同一数据范围与过滤条件） */
    long countApprovedTasks(Long userId, String userRole,
                            String materialType, String keyword);

    /**
     * 已驳回任务列表（最近一次审核操作为 TASK_REJECT，且后续无 TASK_APPROVE）
     *  - BL：看到所有被驳回的任务
     *  - AM：看到自己创建订单下被驳回的任务
     *  - DS：看不到
     */
    CursorPage<OrderTaskVO> listRejectedTasks(Long userId, String userRole,
                                               String cursor, int limit,
                                               String materialType, String keyword);

    /** 已驳回任务数量（与 listRejectedTasks 同一数据范围与过滤条件） */
    long countRejectedTasks(Long userId, String userRole,
                            String materialType, String keyword);

    /** 审核统计：待审核 / 已定稿 / 驳回次数（按角色数据范围） */
    com.peak.vo.AuditStatsVO getAuditStats(Long userId, String userRole);

    /**
     * 我参与的审核记录（我作为操作人的所有审核相关操作）
     * 包括：TASK_APPROVE、TASK_REJECT、ORDER_DELIVER 等
     */
    List<OperationLogVO> listMyAuditLogs(Long userId, String userRole, int limit);

    /**
     * 按订单查审核记录（含任务级和订单级所有审核相关操作）
     */
    List<OperationLogVO> listAuditLogsByOrder(Long orderId, Long userId, String userRole);
}
