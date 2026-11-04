package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmDTO {

    @NotEmpty(message = "物料任务列表不能为空")
    @Valid
    @Schema(description = "确认后的物料任务列表")
    private List<TaskCreateItem> tasks;

    @NotNull(message = "版本号不能为空")
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;
}
