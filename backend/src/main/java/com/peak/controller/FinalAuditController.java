package com.peak.controller;

import com.peak.dto.ArchiveOrderDTO;
import com.peak.dto.FinalAuditDTO;
import com.peak.service.FinalAuditService;
import com.peak.vo.ArchiveOrderResponseVO;
import com.peak.vo.FinalAuditResponseVO;
import com.peak.vo.OrderAuditInfoVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定稿审核与交付归档控制器 — C 模块 M5 + M6
 *
 * <p>BaseURL：/api/v1/final-audit
 * 职责：定稿审核（确认/驳回）+ 订单交付归档 + 定稿归档信息查询
 *
 * <p>当前用户 userId / role 从 JWT 拦截器注入的 request.setAttribute 中读取。
 * 权限校验在 Service 层完成（复用 UserRole 枚举 + 已有的 RBAC 角色体系）。
 *
 * <p>模块边界：本 Controller 仅处理定稿审核与交付归档业务。
 * 文件上传、设计版本管理、客户反馈意见属于 D 模块，不在此处处理。</p>
 */
@RestController
@RequestMapping("/v1/final-audit")
@RequiredArgsConstructor
@Tag(name = "定稿审核与交付归档", description = "M5 定稿审核 + M6 交付归档（C 模块）")
public class FinalAuditController {

    private final FinalAuditService finalAuditService;

    /**
     * 确认定稿 — WAIT_APPROVE → APPROVED
     * POST /api/v1/final-audit/orders/{id}/confirm
     *
     * <p>权限：业务负责人(BL) / 客户经理(AM)
     * <p>前置条件：订单状态为待定稿，且已上传设计版本
     */
    @PostMapping("/orders/{id}/confirm")
    @Operation(summary = "确认定稿（待定稿 → 已定稿）")
    public ResultVO<FinalAuditResponseVO> confirmFinal(
            @PathVariable Long id,
            @Valid @RequestBody FinalAuditDTO dto,
            HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return ResultVO.success(finalAuditService.confirmFinal(id, dto, operatorId));
    }

    /**
     * 驳回定稿 — WAIT_APPROVE → DESIGNING
     * POST /api/v1/final-audit/orders/{id}/reject
     *
     * <p>权限：业务负责人(BL) / 客户经理(AM)
     * <p>驳回后订单回退为设计中，设计师可继续上传新版本
     */
    @PostMapping("/orders/{id}/reject")
    @Operation(summary = "驳回定稿（待定稿 → 设计中）")
    public ResultVO<FinalAuditResponseVO> rejectFinal(
            @PathVariable Long id,
            @Valid @RequestBody FinalAuditDTO dto,
            HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return ResultVO.success(finalAuditService.rejectFinal(id, dto, operatorId));
    }

    /**
     * 订单交付归档 — APPROVED → DELIVERED + archived=1
     * POST /api/v1/final-audit/orders/{id}/archive
     *
     * <p>权限：仅业务负责人(BL)
     * <p>归档后订单变为只读，后续所有修改操作（定稿、驳回、上传版本、提交意见）均被拒绝
     */
    @PostMapping("/orders/{id}/archive")
    @Operation(summary = "交付归档（已定稿 → 已交付 + 归档）")
    public ResultVO<ArchiveOrderResponseVO> archiveOrder(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveOrderDTO dto,
            HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return ResultVO.success(finalAuditService.archiveOrder(id, dto, operatorId));
    }

    /**
     * 查询订单定稿归档信息
     * GET /api/v1/final-audit/orders/{id}
     *
     * <p>用于订单详情页展示定稿记录、归档记录
     * <p>权限：所有登录用户（数据范围由前端订单列表控制）
     */
    @GetMapping("/orders/{id}")
    @Operation(summary = "查询订单定稿归档信息")
    public ResultVO<OrderAuditInfoVO> getAuditInfo(@PathVariable Long id) {
        return ResultVO.success(finalAuditService.getAuditInfo(id));
    }
}
