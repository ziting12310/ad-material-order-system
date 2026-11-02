package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** POST /api/v1/files/presign 入参 — API.md §2.2 */
@Data
public class PresignRequest {
    /** 文件类型：reference（参考文件）/ design（设计稿） */
    @NotBlank(message = "文件类型不能为空")
    private String fileType;
    /** 原始文件名 */
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    /** 文件大小（字节） */
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
    /** 文件 MIME 类型 */
    @NotBlank(message = "MIME 类型不能为空")
    private String contentType;
}
