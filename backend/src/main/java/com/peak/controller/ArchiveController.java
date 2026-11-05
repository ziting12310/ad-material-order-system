package com.peak.controller;

import com.peak.common.context.UserContext;
import com.peak.service.ArchiveService;
import com.peak.vo.CursorPage;
import com.peak.vo.OrderDetailVO;
import com.peak.vo.OrderListItemVO;
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

/**
 * 归档管理控制器 — C 模块（归档域）
 *
 * <p>BaseURL：/api/v1/archive
 * 提供归档订单的查询能力。归档动作由订单交付触发（OrderService.markDelivered）。
 * 所有接口均为只读，归档订单不可修改。
 */
@RestController
@RequestMapping("/v1/archive")
@RequiredArgsConstructor
@Tag(name = "归档管理", description = "已归档订单查询（只读）")
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * 已归档订单列表
     * GET /api/v1/archive/orders?cursor=&limit=20
     */
    @GetMapping("/orders")
    @Operation(summary = "已归档订单列表（Cursor分页）")
    public ResultVO<CursorPage<OrderListItemVO>> listArchivedOrders(
            @Parameter(description = "游标") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页数量，默认20") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "物料类型过滤") @RequestParam(required = false) String materialType,
            @Parameter(description = "关键字（订单号/客户名称）") @RequestParam(required = false) String keyword,
            @Parameter(description = "交付日期起（YYYY-MM-DD）") @RequestParam(required = false) String deliveredDateFrom,
            @Parameter(description = "交付日期止（YYYY-MM-DD）") @RequestParam(required = false) String deliveredDateTo,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(archiveService.listArchivedOrders(
                userId, role, cursor, limit, materialType, keyword, deliveredDateFrom, deliveredDateTo));
    }

    /**
     * 归档订单详情
     * GET /api/v1/archive/orders/{id}
     */
    @GetMapping("/orders/{id}")
    @Operation(summary = "归档订单详情（只读）")
    public ResultVO<OrderDetailVO> getArchivedOrder(@PathVariable Long id,
                                                     HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(archiveService.getArchivedOrderDetail(id, userId, role));
    }

    /**
     * 归档订单数量统计
     * GET /api/v1/archive/count
     */
    @GetMapping("/count")
    @Operation(summary = "归档订单数量统计")
    public ResultVO<Long> countArchived(HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(archiveService.countArchived(userId, role));
    }
}
