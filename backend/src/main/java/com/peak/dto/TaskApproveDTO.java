package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 任务定稿请求 */
@Data
public class TaskApproveDTO {

    @NotNull(message = "version 不能为空")
    @Schema(description = "任务乐观锁版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;

    @Size(max = 500, message = "定稿备注不能超过 500 字")
    @Schema(description = "定稿备注，可选")
    private String remark;
}
