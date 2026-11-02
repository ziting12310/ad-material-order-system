package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** PUT /api/v1/orders/{id}/approve 响应 data */
@Data
public class OrderApproveResponseVO {
    private Long id;
    private String status;
    private Long finalVersionId;
    private LocalDateTime approvedAt;
    private Integer version;
}
