package com.peak.controller;

import com.peak.common.context.UserContext;
import com.peak.common.annotation.RequireRole;
import com.peak.common.enums.UserRole;
import com.peak.dto.OrderApproveRequest;
import com.peak.dto.OrderAssignRequest;
import com.peak.dto.OrderCancelRequest;
import com.peak.dto.OrderConfirmDTO;
import com.peak.dto.OrderCreateRequest;
import com.peak.dto.OrderDeliverRequest;
import com.peak.dto.OrderRejectRequest;
import com.peak.dto.OrderUpdateRequest;
import com.peak.dto.VersionOnlyRequest;
import com.peak.service.OrderService;
import com.peak.vo.CursorPage;
import com.peak.vo.OrderApproveResponseVO;
import com.peak.vo.OrderAssignResponseVO;
import com.peak.vo.OrderCreateResponseVO;
import com.peak.vo.OrderDeliverResponseVO;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderListItemVO;
import com.peak.vo.PageResult;
import com.peak.vo.OrderRejectResponseVO;
import com.peak.vo.OrderStatusUpdateResponseVO;
import com.peak.vo.OperationLogVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单核心控制器 — API.md §3（11 个订单级 Domain Actions 全部路由）
 *
 * <p>BaseURL：/api/v1/orders
 * 职责：HTTP 协议层（路径 + 参数 + 状态码）；业务逻辑全在 OrderServiceImpl。
 * 当前用户 userId / role 从 JWT 拦截器注入的 request.setAttribute 中读取。</p>
 */
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单全生命周期：登记→分派→设计→修改→定稿→交付")
public class OrderController {

    private final OrderService orderService;

    /**
     * 订单列表（Cursor 游标分页）
     * GET /api/v1/orders?cursor=&limit=20&materialType=&status=&keyword=
     *
     * <p>成功响应：
     * <pre>
     * { "code": "SUCCESS", "data": {
     *   "list": [{ "id":1, "orderNo":"ORD-2026-00001", "customerName":"可口可乐", ... }],
     *   "nextCursor": "eyJpZCI6MTAwfQ==",
     *   "hasMore": true
     * }}
     * </pre></p>
     */
    @GetMapping
    @Operation(summary = "订单列表（Cursor 分页）")
    public ResultVO<CursorPage<OrderListItemVO>> listOrders(
            @Parameter(description = "游标（首次请求不传）") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页数量，默认 20，最大 100") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "物料类型过滤") @RequestParam(required = false) String materialType,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "关键字（订单号/客户名称）") @RequestParam(required = false) String keyword,
            @Parameter(description = "设计人员ID过滤") @RequestParam(required = false) Long designerId,
            @Parameter(description = "创建人ID过滤") @RequestParam(required = false) Long creatorId,
            @Parameter(description = "交付日期起（YYYY-MM-DD）") @RequestParam(required = false) String deliveryDateFrom,
            @Parameter(description = "交付日期止（YYYY-MM-DD）") @RequestParam(required = false) String deliveryDateTo,
            @Parameter(description = "物料截止时间起（YYYY-MM-DD）") @RequestParam(required = false) String taskDeadlineFrom,
            @Parameter(description = "物料截止时间止（YYYY-MM-DD）") @RequestParam(required = false) String taskDeadlineTo,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(orderService.listOrders(userId, role, cursor, limit, materialType, status, keyword, designerId, creatorId, deliveryDateFrom, deliveryDateTo, taskDeadlineFrom, taskDeadlineTo));
    }

    /**
     * 订单列表（页码分页 pageNum/pageSize 模式）
     * GET /api/v1/orders/page?pageNum=1&pageSize=20
     *
     * <p>返回：{ list, total, pages, pageNum, pageSize }；数据范围与过滤条件与游标分页接口完全一致。</p>
     */
    @GetMapping("/page")
    @Operation(summary = "订单列表（页码分页）")
    public ResultVO<PageResult<OrderListItemVO>> pageOrders(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量，默认 20，最大 100") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "物料类型过滤") @RequestParam(required = false) String materialType,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "关键字（订单号/客户名称）") @RequestParam(required = false) String keyword,
            @Parameter(description = "设计人员ID过滤") @RequestParam(required = false) Long designerId,
            @Parameter(description = "创建人ID过滤") @RequestParam(required = false) Long creatorId,
            @Parameter(description = "交付日期起（YYYY-MM-DD）") @RequestParam(required = false) String deliveryDateFrom,
            @Parameter(description = "交付日期止（YYYY-MM-DD）") @RequestParam(required = false) String deliveryDateTo,
            @Parameter(description = "物料截止时间起（YYYY-MM-DD）") @RequestParam(required = false) String taskDeadlineFrom,
            @Parameter(description = "物料截止时间止（YYYY-MM-DD）") @RequestParam(required = false) String taskDeadlineTo) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(orderService.pageOrders(userId, role, pageNum, pageSize,
                materialType, status, keyword, designerId, creatorId, deliveryDateFrom, deliveryDateTo, taskDeadlineFrom, taskDeadlineTo));
    }

    /**
     * 订单详情
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "订单详情（含参考文件/设计稿/意见）")
    public ResultVO<OrderDetailVO> getOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(orderService.getOrderDetail(id, userId, role));
    }

    /**
     * 创建订单 — US-001 客户经理登记
     * POST /api/v1/orders
     *
     * <p>请求 JSON：
     * <pre>
     * {
     *   "customerName": "可口可乐", "contactName": "李四",
     *   "contactPhone": "13800138000", "materialType": "POSTER",
     *   "quantity": 1000, "specification": "A3 尺寸，300g 铜版纸",
     *   "deliveryDate": "2026-11-15", "remark": "双面印刷",
     *   "referenceFileIds": ["file_presign_001"]
     * }
     * </pre></p>
     */
    @PostMapping
    @RequireRole({UserRole.AM})
    @Operation(summary = "创建订单")
    public ResultVO<OrderCreateResponseVO> createOrder(@Valid @RequestBody OrderCreateRequest dto,
                                                       HttpServletRequest request) {
        Long creatorId = UserContext.getUserId();
        return ResultVO.success(orderService.createOrder(dto, creatorId));
    }

    /**
     * 编辑订单 — 仅 CREATED 状态
     * PUT /api/v1/orders/{id}
     */
    @PutMapping("/{id}")
    @RequireRole({UserRole.AM, UserRole.BL})
    @Operation(summary = "编辑订单（仅已登记状态可编辑）")
    public ResultVO<OrderCreateResponseVO> updateOrder(@PathVariable Long id,
                                                       @Valid @RequestBody OrderUpdateRequest dto,
                                                       HttpServletRequest request) {
        Long operatorId = UserContext.getUserId();
        return ResultVO.success(orderService.updateOrder(id, dto, operatorId));
    }

    /**
     * 分派设计人员 — US-002 业务负责人分派
     * PUT /api/v1/orders/{id}/assign
     *
     * <p>请求 JSON：
     * <pre>{"designerId": 3, "assignRemark": "优先处理", "version": 0}</pre></p>
     */
    @PutMapping("/{id}/assign")
    @RequireRole({UserRole.BL})
    @Operation(summary = "分派设计人员（CREATED → ASSIGNED）")
    public ResultVO<OrderAssignResponseVO> assignOrder(@PathVariable Long id,
                                                       @Valid @RequestBody OrderAssignRequest dto,
                                                       HttpServletRequest request) {
        Long operatorId = UserContext.getUserId();
        return ResultVO.success(orderService.assignOrder(id, dto, operatorId));
    }

    /**
     * 设计师接单 — US-003 设计人员开始工作
     * PUT /api/v1/orders/{id}/accept
     *
     * <p>请求 JSON：<pre>{"version": 1}</pre></p>
     */
    @PutMapping("/{id}/accept")
    @RequireRole({UserRole.DS})
    @Operation(summary = "设计师接单（ASSIGNED → DESIGNING）")
    public ResultVO<OrderStatusUpdateResponseVO> acceptTask(@PathVariable Long id,
                                                            @Valid @RequestBody VersionOnlyRequest dto,
                                                            HttpServletRequest request) {
        Long designerId = UserContext.getUserId();
        return ResultVO.success(orderService.acceptTask(id, dto, designerId));
    }

    /**
     * 驳回设计稿 — US-005 客户经理驳回后自动生成 Feedback
     * PUT /api/v1/orders/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @RequireRole({UserRole.AM})
    @Operation(summary = "驳回设计稿（WAIT_APPROVE → DESIGNING）")
    public ResultVO<OrderRejectResponseVO> rejectDesign(@PathVariable Long id,
                                                       @Valid @RequestBody OrderRejectRequest dto,
                                                       HttpServletRequest request) {
        Long operatorId = UserContext.getUserId();
        return ResultVO.success(orderService.rejectDesign(id, dto, operatorId));
    }

    /**
     * 确认终稿 — US-007 客户经理定稿 + 标记 final 版本
     * PUT /api/v1/orders/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @RequireRole({UserRole.AM})
    @Operation(summary = "确认终稿（WAIT_APPROVE → APPROVED）")
    public ResultVO<OrderApproveResponseVO> approveFinal(@PathVariable Long id,
                                                          @Valid @RequestBody OrderApproveRequest dto,
                                                          HttpServletRequest request) {
        Long operatorId = UserContext.getUserId();
        return ResultVO.success(orderService.approveFinal(id, dto, operatorId));
    }

    /**
     * 标记交付 — US-007 业务负责人交付 + 自动归档 archived=1
     * PUT /api/v1/orders/{id}/deliver
     */
    @PutMapping("/{id}/deliver")
    @RequireRole({UserRole.BL})
    @Operation(summary = "标记交付（APPROVED → DELIVERED + 归档）")
    public ResultVO<OrderDeliverResponseVO> markDelivered(@PathVariable Long id,
                                                          @Valid @RequestBody OrderDeliverRequest dto,
                                                          HttpServletRequest request) {
        Long operatorId = UserContext.getUserId();
        return ResultVO.success(orderService.markDelivered(id, dto, operatorId));
    }

    /**
     * 作废订单 — 任意非终态 → CANCELLED
     * PUT /api/v1/orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @RequireRole({UserRole.AM, UserRole.BL})
    @Operation(summary = "作废订单（任意非终态 → CANCELLED）")
    public ResultVO<OrderStatusUpdateResponseVO> cancelOrder(@PathVariable Long id,
                                                             @Valid @RequestBody OrderCancelRequest dto,
                                                             HttpServletRequest request) {
        Long operatorId = UserContext.getUserId();
        return ResultVO.success(orderService.cancelOrder(id, dto, operatorId));
    }

    /**
     * 确认物料清单 — BL 确认订单物料清单和截止时间
     * PUT /api/v1/orders/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    @RequireRole({UserRole.BL})
    @Operation(summary = "确认物料清单（BL确认后订单进入可分派状态）")
    public ResultVO<Void> confirmOrder(@PathVariable Long id,
                                        @Valid @RequestBody OrderConfirmDTO dto,
                                        HttpServletRequest request) {
        Long operatorId = UserContext.getUserId();
        orderService.confirmOrder(id, dto, operatorId);
        return ResultVO.success(null);
    }

    /**
     * 删除订单 — 仅 AM（客户经理）且订单处于待分派（CREATED）
     * DELETE /api/v1/orders/{id}
     */
    @DeleteMapping("/{id}")
    @RequireRole({UserRole.AM})
    @Operation(summary = "删除订单（仅待分派状态，逻辑删除）")
    public ResultVO<Void> deleteOrder(@PathVariable Long id, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        orderService.deleteOrder(id, operatorId);
        return ResultVO.success(null);
    }

    /**
     * 订单操作历史
     * GET /api/v1/orders/{id}/logs
     */
    @GetMapping("/{id}/logs")
    @Operation(summary = "查询订单操作历史")
    public ResultVO<java.util.List<OperationLogVO>> listOperationLogs(@PathVariable Long id,
                                                                        HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(orderService.listOperationLogs(id, userId, role));
    }
}
