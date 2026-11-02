package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** POST /api/v1/orders 响应 data */
@Data
public class OrderCreateResponseVO {
    private Long id;
    private String orderNo;
    private String status;
    private LocalDateTime createdAt;
}
