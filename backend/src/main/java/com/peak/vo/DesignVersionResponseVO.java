package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** POST /api/v1/design-versions 响应 — 首版 / 新版本共用 */
@Data
public class DesignVersionResponseVO {
    private Long id;
    private Long taskId;
    private String versionNo;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String versionNote;
    private Boolean isFinal;
    private LocalDateTime uploadedAt;
    private String orderStatus;
    private Integer orderVersion;
}
