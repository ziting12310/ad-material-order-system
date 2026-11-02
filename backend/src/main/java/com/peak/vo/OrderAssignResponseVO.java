package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** PUT /api/v1/orders/{id}/assign 响应 data */
@Data
public class OrderAssignResponseVO {
    private Long id;
    private String status;
    private Long designerId;
    private String designerName;
    private LocalDateTime assignedAt;
    private Integer version;
}
