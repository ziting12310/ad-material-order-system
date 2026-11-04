package com.peak.controller;

import com.peak.dto.OrderFeedbackCreateRequest;
import com.peak.service.OrderFeedbackService;
import com.peak.vo.OrderFeedbackResponseVO;
import com.peak.vo.OrderFeedbackVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 修改意见控制器 — API.md §5.1
 *
 * <p>BaseURL：/api/v1/order-feedbacks
 * 职责：客户经理/业务负责人提交修改意见 → 订单状态回退为 DESIGNING（触发设计人员继续修改）</p>
 */
@RestController
@RequestMapping("/v1/order-feedbacks")
@RequiredArgsConstructor
@Tag(name = "修改意见", description = "提交设计修改意见")
public class OrderFeedbackController {

    private final OrderFeedbackService orderFeedbackService;

    /**
     * 提交修改意见 — US-005 / US-006
     * POST /api/v1/order-feedbacks
     *
     * <p>请求 JSON：
     * <pre>
     * { "orderId": 1, "content": "标题位置需要上移 20px，字体加粗", "version": 2 }
     * </pre></p>
     *
     * <p>成功响应：
     * <pre>
     * { "code": "SUCCESS", "data": {
     *   "id": 3, "status": "PENDING", "orderStatus": "DESIGNING", "orderVersion": 4
     * }}
     * </pre></p>
     */
    @PostMapping
    @Operation(summary = "提交修改意见（WAIT_APPROVE → DESIGNING）")
    public ResultVO<OrderFeedbackResponseVO> submitFeedback(
            @Valid @RequestBody OrderFeedbackCreateRequest dto,
            HttpServletRequest request) {
        Long submitterId = (Long) request.getAttribute("userId");
        return ResultVO.success(orderFeedbackService.submitFeedback(dto, submitterId));
    }

    /**
     * 意见列表 — GET /api/v1/order-feedbacks?taskId=1
     * <p>按任务查所有修改意见；也兼容只传 orderId。按提交时间升序。</p>
     */
    @GetMapping
    @Operation(summary = "按任务 / 订单查询修改意见")
    public ResultVO<List<OrderFeedbackVO>> listFeedbacks(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long taskId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("userRole");
        return ResultVO.success(orderFeedbackService.listFeedbacks(orderId, taskId, userId, userRole));
    }
}
