package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /api/v1/orders/{id}/cancel 入参（作废订单）— API.md §3.10 */
@Data
public class OrderCancelRequest {
    /** 作废原因（必填，1~200 字符） */
    @NotBlank(message = "作废原因不能为空")
    @Size(min = 1, max = 200, message = "作废原因长度 1~200 字符")
    private String cancelReason;
    @NotNull(message = "version 不能为空")
    private Integer version;
}
