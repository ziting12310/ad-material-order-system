package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /api/v1/orders/{id}/reject 入参（驳回设计稿，必须填写原因）— API.md §3.7 */
@Data
public class OrderRejectRequest {
    /** 驳回原因（必填，1~500 字符） */
    @NotBlank(message = "驳回原因不能为空")
    @Size(min = 1, max = 500, message = "驳回原因长度 1~500 字符")
    private String rejectNote;
    @NotNull(message = "version 不能为空")
    private Integer version;
}
