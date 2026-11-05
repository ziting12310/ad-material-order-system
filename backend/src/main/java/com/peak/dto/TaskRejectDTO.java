package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 任务驳回请求 */
@Data
public class TaskRejectDTO {

    @NotNull(message = "version 不能为空")
    @Schema(description = "任务乐观锁版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;

    @NotBlank(message = "驳回说明不能为空")
    @Size(max = 200, message = "驳回说明不能超过 200 字")
    @Schema(description = "驳回说明，会写入任务修改意见", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rejectNote;
}
