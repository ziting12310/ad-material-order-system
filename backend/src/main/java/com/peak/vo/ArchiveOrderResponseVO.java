package com.peak.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单交付归档响应 VO — C 模块 M6 交付归档专用
 *
 * <p>归档成功后订单进入 DELIVERED 状态且 archived=1，变为只读。</p>
 */
@Data
@Schema(description = "订单交付归档响应")
public class ArchiveOrderResponseVO {

    @Schema(description = "订单ID", example = "1")
    private Long id;

    @Schema(description = "订单编号", example = "ORD-2026-00001")
    private String orderNo;

    @Schema(description = "订单状态（归档后为 DELIVERED）", example = "DELIVERED")
    private String status;

    @Schema(description = "乐观锁版本号（更新后）", example = "6")
    private Integer version;

    @Schema(description = "是否已归档", example = "true")
    private Boolean isArchived;

    @Schema(description = "归档/交付人ID", example = "3")
    private Long archivedBy;

    @Schema(description = "归档/交付人姓名", example = "王五")
    private String archivedByName;

    @Schema(description = "归档/交付时间")
    private LocalDateTime archivedAt;

    @Schema(description = "归档备注", example = "已交付客户，确认无问题")
    private String archiveRemark;
}
