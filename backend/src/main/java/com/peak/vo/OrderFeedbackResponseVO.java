package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** POST /api/v1/order-feedbacks 响应 */
@Data
public class OrderFeedbackResponseVO {
    private Long id;
    private String content;
    private String status;
    private Long versionId;
    private String versionNo;
    private LocalDateTime submittedAt;
    private String orderStatus;
    private Integer orderVersion;
}
