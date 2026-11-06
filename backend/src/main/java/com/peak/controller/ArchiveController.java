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
 * 归档管理控制器 — 只读
 */
@RestController
@RequestMapping("/v1/archive")
@RequiredArgsConstructor
@Tag(name = "归档管理", description = "已归档订单查询（只读）")
public class ArchiveController {

    private final ArchiveService archiveService;

    /** 已归档订单列表 */
    @GetMapping("/orders")
    @Operation(summary = "已归档订单列表（Cursor分页）")
    public ResultVO<CursorPage<OrderListItemVO>> listArchivedOrders(
            @Parameter(description = "游标") @RequestParam(required = false) String cursor,
            @Parameter(description = "每页数量，默认20") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "物料类型过滤") @RequestParam(required = false) String materialType,
            @Parameter(description = "关键字（公司名称模糊）") @RequestParam(required = false) String keyword,
            @Parameter(description = "交付日期起（YYYY-MM-DD）") @RequestParam(required = false) String deliveredDateFrom,
            @Parameter(description = "交付日期止（YYYY-MM-DD）") @RequestParam(required = false) String deliveredDateTo,
            @Parameter(description = "物料截止时间起（YYYY-MM-DD）") @RequestParam(required = false) String taskDeadlineFrom,
            @Parameter(description = "物料截止时间止（YYYY-MM-DD）") @RequestParam(required = false) String taskDeadlineTo,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(archiveService.listArchivedOrders(
                userId, role, cursor, limit, materialType, keyword, deliveredDateFrom, deliveredDateTo,
                taskDeadlineFrom, taskDeadlineTo));
    }

    /** 归档订单详情 */
    @GetMapping("/orders/{id}")
    @Operation(summary = "归档订单详情（只读）")
    public ResultVO<OrderDetailVO> getArchivedOrder(@PathVariable Long id,
                                                     HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(archiveService.getArchivedOrderDetail(id, userId, role));
    }

    /** 归档订单数量统计 */
    @GetMapping("/count")
    @Operation(summary = "归档订单数量统计")
    public ResultVO<Long> countArchived(
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deliveredDateFrom,
            @RequestParam(required = false) String deliveredDateTo,
            @RequestParam(required = false) String taskDeadlineFrom,
            @RequestParam(required = false) String taskDeadlineTo,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return ResultVO.success(archiveService.countArchived(
                userId, role, materialType, keyword, deliveredDateFrom, deliveredDateTo,
                taskDeadlineFrom, taskDeadlineTo));
    }
}
