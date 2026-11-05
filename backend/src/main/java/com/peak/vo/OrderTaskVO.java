package com.peak.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderTaskVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务编号", example = "T-001")
    private String taskNo;

    @Schema(description = "订单号", example = "ORD-2024-001")
    private String orderNo;

    @Schema(description = "客户名称（公司）")
    private String customerName;

    @Schema(description = "物料类型", example = "POSTER")
    private String materialType;

    @Schema(description = "物料类型名称", example = "海报")
    private String materialTypeName;

    @Schema(description = "数量", example = "500")
    private Integer quantity;

    @Schema(description = "规格说明")
    private String specification;

    @Schema(description = "用途说明")
    private String usageDesc;

    @Schema(description = "截止日期")
    private LocalDate deadline;

    @Schema(description = "任务备注")
    private String remark;

    @Schema(description = "任务状态", example = "DESIGNING")
    private String status;

    @Schema(description = "状态名称", example = "设计中")
    private String statusName;

    @Schema(description = "设计人员ID")
    private Long designerId;

    @Schema(description = "设计人员姓名")
    private String designerName;

    @Schema(description = "分派备注")
    private String assignRemark;

    @Schema(description = "定稿人姓名")
    private String approvedByName;

    @Schema(description = "定稿时间")
    private LocalDateTime approvedAt;

    @Schema(description = "定稿版本号", example = "V3")
    private String finalVersionNo;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
