package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateDTO {

    @NotNull(message = "版本号不能为空")
    @Schema(description = "乐观锁版本号", example = "1")
    private Integer version;

    @Schema(description = "物料类型")
    private String materialType;

    @Schema(description = "数量")
    private Integer quantity;

    @Size(min = 2, max = 200, message = "规格说明长度需在 2-200 字符之间")
    @Schema(description = "规格说明")
    private String specification;

    @Size(max = 200, message = "用途说明最多200字符")
    @Schema(description = "用途说明")
    private String usageDesc;

    @Schema(description = "截止日期")
    private LocalDate deadline;

    @Size(max = 500, message = "备注最多500字符")
    @Schema(description = "任务备注")
    private String remark;
}
