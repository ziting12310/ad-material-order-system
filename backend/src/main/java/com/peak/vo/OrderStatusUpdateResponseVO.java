package com.peak.vo;

import lombok.Data;

/** 通用状态流转响应 — AcceptTask / MarkDelivered / CancelOrder 共用 */
@Data
public class OrderStatusUpdateResponseVO {
    private Long id;
    private String status;
    private Integer version;
}
