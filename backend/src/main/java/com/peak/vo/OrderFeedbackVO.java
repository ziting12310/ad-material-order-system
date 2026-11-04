package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 修改意见 VO — OrderDetailVO.feedbacks 子项 */
@Data
public class OrderFeedbackVO {
    private Long id;
    private Long taskId;
    private String content;
    private String status;
    private Long versionId;
    private String versionNo;
    private String submittedByName;
    private LocalDateTime submittedAt;
}
