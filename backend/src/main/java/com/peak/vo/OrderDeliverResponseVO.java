package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** PUT /api/v1/orders/{id}/deliver 响应 data */
@Data
public class OrderDeliverResponseVO {
    private Long id;
    private String status;
    private LocalDateTime deliveredAt;
    private Boolean isArchived;
    private Integer version;
}
