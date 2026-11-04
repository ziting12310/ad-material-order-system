package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskAssignDTO {

    @NotNull(message = "设计人员ID不能为空")
    @Schema(description = "设计人员ID", example = "4")
    private Long designerId;

    @Size(max = 200, message = "分派备注最多200字符")
    @Schema(description = "分派备注", example = "优先处理")
    private String assignRemark;

    @NotNull(message = "版本号不能为空")
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;
}
