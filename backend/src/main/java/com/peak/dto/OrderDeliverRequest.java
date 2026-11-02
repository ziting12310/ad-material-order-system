package com.peak.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /api/v1/orders/{id}/deliver 入参（标记交付）— API.md §3.9 */
@Data
public class OrderDeliverRequest {
    /** 交付备注（选填，≤100 字符） */
    @Size(max = 100, message = "交付备注不能超过 100 字符")
    private String deliverRemark;
    @NotNull(message = "version 不能为空")
    private Integer version;
}
