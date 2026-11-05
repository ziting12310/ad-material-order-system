package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 设计稿版本 VO — OrderDetailVO.designVersions 子项 */
@Data
public class DesignVersionVO {
    private Long id;
    private Long taskId;
    private String fileId;
    private String versionNo;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String versionNote;
    private Boolean isFinal;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
