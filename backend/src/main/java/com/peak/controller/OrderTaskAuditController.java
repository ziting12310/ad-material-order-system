package com.peak.controller;

import com.peak.common.annotation.RequireRole;
import com.peak.common.context.UserContext;
import com.peak.common.enums.UserRole;
import com.peak.dto.TaskApproveDTO;
import com.peak.dto.TaskRejectDTO;
import com.peak.service.OrderTaskService;
import com.peak.vo.OrderTaskVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务级定稿 / 驳回控制器 — A 模块
 *
 * <p>BaseURL：/api/v1/order-tasks（yml context-path=/api + 本类 @RequestMapping("/v1/order-tasks")）。</p>
 * <p>OrderTaskController 的类级路径是 /v1/orders/{orderId}/tasks，无法在同一类中
 * 暴露 /v1/order-tasks/{taskId}/...，因此单独拆一个控制器，复用已有 Service 方法。</p>
 */
@RestController
@RequestMapping("/v1/order-tasks")
@RequiredArgsConstructor
@Tag(name = "任务定稿审核", description = "任务级确认定稿 / 驳回")
public class OrderTaskAuditController {

    private final OrderTaskService orderTaskService;

    /** POST /api/v1/order-tasks/{taskId}/approve */
    @PostMapping("/{taskId}/approve")
    @RequireRole({UserRole.AM})
    @Operation(summary = "确认任务定稿", description = "WAIT_APPROVE → APPROVED，仅 AM 可操作")
    public ResultVO<OrderTaskVO> approveTask(@PathVariable Long taskId,
                                             @Valid @RequestBody TaskApproveDTO dto) {
        return ResultVO.success(orderTaskService.approveTask(
                taskId, dto.getRemark(), dto.getVersion(), UserContext.getUserId()));
    }

    /** POST /api/v1/order-tasks/{taskId}/reject */
    @PostMapping("/{taskId}/reject")
    @RequireRole({UserRole.AM})
    @Operation(summary = "驳回任务定稿", description = "WAIT_APPROVE → DESIGNING，并自动生成修改意见，仅 AM 可操作")
    public ResultVO<OrderTaskVO> rejectTask(@PathVariable Long taskId,
                                            @Valid @RequestBody TaskRejectDTO dto) {
        return ResultVO.success(orderTaskService.rejectTask(
                taskId, dto.getRejectNote(), dto.getVersion(), UserContext.getUserId()));
    }
}
