package com.peak.controller;

import com.peak.dto.TaskAssignDTO;
import com.peak.dto.TaskCreateItem;
import com.peak.dto.TaskUpdateDTO;
import com.peak.service.OrderTaskService;
import com.peak.vo.OrderStatusUpdateResponseVO;
import com.peak.vo.OrderTaskVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/orders/{orderId}/tasks")
@RequiredArgsConstructor
@Tag(name = "物料任务管理", description = "订单下的物料任务：增删改查 + 分派 + 设计 + 定稿")
public class OrderTaskController {

    private final OrderTaskService orderTaskService;

    @GetMapping
    @Operation(summary = "查询订单下所有物料任务")
    public ResultVO<List<OrderTaskVO>> listTasks(@PathVariable Long orderId,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");
        return ResultVO.success(orderTaskService.listTasks(orderId, userId, role));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询单个物料任务详情")
    public ResultVO<OrderTaskVO> getTask(@PathVariable Long orderId,
                                          @PathVariable Long taskId,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");
        return ResultVO.success(orderTaskService.getTask(taskId, userId, role));
    }

    @PostMapping
    @Operation(summary = "新增物料任务")
    public ResultVO<OrderTaskVO> addTask(@PathVariable Long orderId,
                                          @Valid @RequestBody TaskCreateItem dto,
                                          HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return ResultVO.success(orderTaskService.addTask(orderId, dto, operatorId));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "修改物料任务（仅 CREATED 可改）")
    public ResultVO<OrderTaskVO> updateTask(@PathVariable Long orderId,
                                             @PathVariable Long taskId,
                                             @Valid @RequestBody TaskUpdateDTO dto,
                                             HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return ResultVO.success(orderTaskService.updateTask(taskId, dto, operatorId));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除物料任务（仅 CREATED 可删）")
    public ResultVO<Void> removeTask(@PathVariable Long orderId,
                                      @PathVariable Long taskId,
                                      HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        orderTaskService.removeTask(taskId, operatorId);
        return ResultVO.success(null);
    }

    @PutMapping("/{taskId}/assign")
    @Operation(summary = "分派设计人员（CREATED → ASSIGNED）")
    public ResultVO<OrderTaskVO> assignTask(@PathVariable Long orderId,
                                             @PathVariable Long taskId,
                                             @Valid @RequestBody TaskAssignDTO dto,
                                             HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return ResultVO.success(orderTaskService.assignTask(taskId, dto, operatorId));
    }

    @PutMapping("/{taskId}/accept")
    @Operation(summary = "设计师接单（ASSIGNED → DESIGNING）")
    public ResultVO<OrderStatusUpdateResponseVO> acceptTask(@PathVariable Long orderId,
                                                              @PathVariable Long taskId,
                                                              @RequestBody AcceptBody body,
                                                              HttpServletRequest request) {
        Long designerId = (Long) request.getAttribute("userId");
        return ResultVO.success(orderTaskService.acceptTask(taskId, body.getVersion(), designerId));
    }

    @Data
    public static class AcceptBody {
        private Integer version;
    }
}
