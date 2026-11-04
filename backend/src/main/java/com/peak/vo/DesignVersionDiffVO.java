package com.peak.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 设计稿版本对比 VO — GET /v1/design-versions/diff */
@Data
public class DesignVersionDiffVO {

    private Long orderId;
    private Long taskId;
    private VersionItem v1;
    private VersionItem v2;

    /** 单个版本的文件信息（前端并排展示用） */
    @Data
    public static class VersionItem {
        private Long id;
        private String versionNo;
        private String fileUrl;
        private String fileName;
        private Long fileSize;
        private String versionNote;
        private LocalDateTime uploadedAt;
    }
}
