package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskCreateItem {

    @NotBlank(message = "物料类型不能为空")
    @Schema(description = "物料类型", example = "POSTER")
    private String materialType;

    @NotNull(message = "数量不能为空")
    @Schema(description = "数量", example = "500")
    private Integer quantity;

    @NotBlank(message = "规格说明不能为空")
    @Size(min = 2, max = 200, message = "规格说明长度需在 2-200 字符之间")
    @Schema(description = "规格说明", example = "A3尺寸 300g铜版纸 双面印刷")
    private String specification;

    @Size(max = 200, message = "用途说明最多200字符")
    @Schema(description = "用途说明", example = "门店开业宣传用")
    private String usageDesc;

    @NotNull(message = "截止日期不能为空")
    @Schema(description = "任务截止日期", example = "2026-11-20")
    private LocalDate deadline;

    @Size(max = 500, message = "备注最多500字符")
    @Schema(description = "任务备注")
    private String remark;
}
