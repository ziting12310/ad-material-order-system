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
    /** 文件扩展名（小写，如 png/psd/pdf），供前端判断预览方式 */
    private String fileType;
    private String versionNote;
    private Boolean isFinal;
    /** 上传人姓名（与 uploadedByName 同值，供前端表格直接渲染） */
    private String uploaderName;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
