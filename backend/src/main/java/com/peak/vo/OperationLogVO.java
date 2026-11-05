package com.peak.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号", example = "T-001")
    private String taskNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号", example = "ORD-2024-001")
    private String orderNo;

    @Schema(description = "操作类型", example = "TASK_APPROVE")
    private String operationType;

    @Schema(description = "操作类型名称", example = "确认定稿")
    private String operationTypeName;

    @Schema(description = "操作描述", example = "确认海报定稿（V3版本）")
    private String description;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名", example = "王五")
    private String operatorName;

    @Schema(description = "操作人角色", example = "BL")
    private String operatorRole;

    @Schema(description = "操作人角色名称", example = "业务负责人")
    private String operatorRoleName;

    @Schema(description = "操作时间")
    private LocalDateTime operatedAt;
}
