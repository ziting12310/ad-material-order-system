package com.peak.controller;

import com.peak.common.context.UserContext;
import com.peak.service.FinalAuditService;
import com.peak.vo.AuditStatsVO;
import com.peak.vo.CursorPage;
import com.peak.vo.OperationLogVO;
import com.peak.vo.OrderTaskVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 定稿审核控制器 — C 模块（审核域）
 *
 * <p>BaseURL：/api/v1/audit
 * 提供审核工作台视角的查询能力：待审核列表、审核记录等。
 * 定稿/驳回的写操作由 OrderTaskController（任务级）和 OrderController（订单级）处理。
 */
@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@Tag(name = "定稿审核", description = "审核工作台：待办列表、审核记录")
public class FinalAuditController {

    private final FinalAuditService finalAuditService;

    /**
     * 待审核任务列表（我的待办）
     * GET /api/v1/audit/pending?cursor=&limit=20
     *
     * <p>BL 看到所有待审核任务；AM 看到自己订单下的待审核任务；DS 无权限。
     */
    @GetMapping("/pending")
    @Operation(summary = "待审核任务列表（我的待办）")
    public ResultVO<CursorPage<OrderTaskVO>> listPendingTasks(
            @Parameter(description = "游标") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页数量，默认20") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "物料类型过滤") @RequestParam(required = false) String materialType,
            @Parameter(description = "关键字（任务编号/规格）") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(finalAuditService.listPendingTasks(
                userId, role, cursor, limit, materialType, keyword));
    }

    /**
     * 待审核任务数量
     * GET /api/v1/audit/pending/count
     */
    @GetMapping("/pending/count")
    @Operation(summary = "待审核任务数量")
    public ResultVO<Long> countPendingTasks(
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");
        return ResultVO.success(finalAuditService.countPendingTasks(userId, role, materialType, keyword));
    }

    /**
     * 已定稿任务列表（APPROVED / DELIVERED 状态）
     * GET /api/v1/audit/approved?cursor=&limit=20
     */
    @GetMapping("/approved")
    @Operation(summary = "已定稿任务列表")
    public ResultVO<CursorPage<OrderTaskVO>> listApprovedTasks(
            @Parameter(description = "游标") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页数量，默认20") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "物料类型过滤") @RequestParam(required = false) String materialType,
            @Parameter(description = "关键字（任务编号/规格）") @RequestParam(required = false) String keyword,
            @Parameter(description = "交付日期起 YYYY-MM-DD") @RequestParam(required = false) String deliveryDateFrom,
            @Parameter(description = "交付日期止 YYYY-MM-DD") @RequestParam(required = false) String deliveryDateTo,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(finalAuditService.listApprovedTasks(
                userId, role, cursor, limit, materialType, keyword, deliveryDateFrom, deliveryDateTo));
    }

    /**
     * 已定稿任务数量（与列表接口完全相同的查询条件）
     * GET /api/v1/audit/approved/count
     */
    @GetMapping("/approved/count")
    @Operation(summary = "已定稿任务数量")
    public ResultVO<Long> countApprovedTasks(
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deliveryDateFrom,
            @RequestParam(required = false) String deliveryDateTo,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(finalAuditService.countApprovedTasks(
                userId, role, materialType, keyword, deliveryDateFrom, deliveryDateTo));
    }

    /**
     * 已驳回任务列表（最近一次审核操作为 TASK_REJECT，后续无 TASK_APPROVE）
     * GET /api/v1/audit/rejected?cursor=&limit=20
     */
    @GetMapping("/rejected")
    @Operation(summary = "已驳回任务列表")
    public ResultVO<CursorPage<OrderTaskVO>> listRejectedTasks(
            @Parameter(description = "游标") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页数量，默认20") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "物料类型过滤") @RequestParam(required = false) String materialType,
            @Parameter(description = "关键字（任务编号/规格）") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(finalAuditService.listRejectedTasks(
                userId, role, cursor, limit, materialType, keyword));
    }

    /**
     * 已驳回任务数量
     * GET /api/v1/audit/rejected/count
     */
    @GetMapping("/rejected/count")
    @Operation(summary = "已驳回任务数量")
    public ResultVO<Long> countRejectedTasks(
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(finalAuditService.countRejectedTasks(userId, role, materialType, keyword));
    }

    /**
     * 审核统计：待审核 / 已定稿 / 驳回次数
     * GET /api/v1/audit/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "审核统计（待审核 / 已定稿 / 驳回次数）")
    public ResultVO<AuditStatsVO> getAuditStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");
        return ResultVO.success(finalAuditService.getAuditStats(userId, role));
    }

    /**
     * 我参与的审核记录
     * GET /api/v1/audit/my-logs?limit=20
     */
    @GetMapping("/my-logs")
    @Operation(summary = "我参与的审核记录")
    public ResultVO<List<OperationLogVO>> listMyAuditLogs(
            @Parameter(description = "数量，默认20，最大100") @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(finalAuditService.listMyAuditLogs(userId, role, limit));
    }

    /**
     * 订单审核记录
     * GET /api/v1/audit/orders/{orderId}/logs
     */
    @GetMapping("/orders/{orderId}/logs")
    @Operation(summary = "查询订单的审核记录")
    public ResultVO<List<OperationLogVO>> listAuditLogsByOrder(
            @PathVariable Long orderId,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(finalAuditService.listAuditLogsByOrder(orderId, userId, role));
    }
}
