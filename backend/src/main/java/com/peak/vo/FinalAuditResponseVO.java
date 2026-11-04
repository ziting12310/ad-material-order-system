package com.peak.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定稿审核响应 VO — confirmFinal / rejectFinal 共用
 *
 * <p>C 模块 M5 定稿审核专用。返回订单当前状态、版本号、以及操作相关的审计字段。</p>
 */
@Data
@Schema(description = "定稿审核响应（确认/驳回）")
public class FinalAuditResponseVO {

    @Schema(description = "订单ID", example = "1")
    private Long id;

    @Schema(description = "订单编号", example = "ORD-2026-00001")
    private String orderNo;

    @Schema(description = "订单状态", example = "APPROVED")
    private String status;

    @Schema(description = "乐观锁版本号（更新后）", example = "4")
    private Integer version;

    @Schema(description = "定稿人ID（仅确认定稿时有值）", example = "3")
    private Long approvedBy;

    @Schema(description = "定稿人姓名（仅确认定稿时有值）", example = "王五")
    private String approvedByName;

    @Schema(description = "定稿时间（仅确认定稿时有值）")
    private LocalDateTime approvedAt;

    @Schema(description = "定稿版本ID（仅确认定稿时有值）", example = "5")
    private Long finalVersionId;

    @Schema(description = "定稿版本号（仅确认定稿时有值）", example = "V2")
    private String finalVersionNo;
}
