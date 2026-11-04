package com.peak.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单审核与归档信息 VO — C 模块配套查询
 *
 * <p>用于订单详情页展示定稿记录、归档记录。
 * 数据全部来自 orders 表现有字段（approved_by/at、final_version_id、
 * delivered_by/at、deliver_remark、is_archived），不依赖额外日志表。</p>
 */
@Data
@Schema(description = "订单定稿归档信息")
public class OrderAuditInfoVO {

    // ---------- 定稿信息 ----------

    @Schema(description = "是否已定稿", example = "true")
    private Boolean isApproved;

    @Schema(description = "定稿人ID", example = "3")
    private Long approvedBy;

    @Schema(description = "定稿人姓名", example = "王五")
    private String approvedByName;

    @Schema(description = "定稿时间")
    private LocalDateTime approvedAt;

    @Schema(description = "定稿版本ID", example = "5")
    private Long finalVersionId;

    @Schema(description = "定稿版本号", example = "V2")
    private String finalVersionNo;

    // ---------- 归档信息 ----------

    @Schema(description = "是否已归档（归档后订单只读）", example = "true")
    private Boolean isArchived;

    @Schema(description = "归档/交付人ID", example = "3")
    private Long archivedBy;

    @Schema(description = "归档/交付人姓名", example = "王五")
    private String archivedByName;

    @Schema(description = "归档/交付时间")
    private LocalDateTime archivedAt;

    @Schema(description = "归档/交付备注", example = "已交付客户，确认无问题")
    private String archiveRemark;
}
