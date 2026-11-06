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
    /** 提交人姓名（与 submittedByName 同值，供前端直接渲染） */
    private String submitterName;
    private String submittedByName;
    private LocalDateTime submittedAt;
}
