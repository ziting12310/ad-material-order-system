package com.peak.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /api/v1/orders/{id}/approve 入参（确认终稿）— API.md §3.8 */
@Data
public class OrderApproveRequest {
    /** 审核意见（选填，≤200 字符） */
    @Size(max = 200, message = "审核意见不能超过 200 字符")
    private String approveNote;
    @NotNull(message = "version 不能为空")
    private Integer version;
}
