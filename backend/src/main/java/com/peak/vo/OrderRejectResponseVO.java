package com.peak.vo;

import lombok.Data;

/** PUT /api/v1/orders/{id}/reject 响应 — 驳回自动生成一条 Feedback */
@Data
public class OrderRejectResponseVO {
    private Long id;
    private String status;
    private Long feedbackId;
    private Integer version;
}
