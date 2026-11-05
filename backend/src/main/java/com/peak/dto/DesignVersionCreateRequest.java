package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /api/v1/design-versions 入参（上传设计稿，首版 / 新版本共用）— API.md §4.1 */
@Data
public class DesignVersionCreateRequest {
    /** 关联订单 ID */
    @NotNull(message = "orderId 不能为空")
    private Long orderId;
    /** 任务 ID（多物料模型下必填，必须属于 orderId） */
    @NotNull(message = "taskId 不能为空")
    private Long taskId;
    /** 设计稿文件 ID（presign 接口返回） */
    @NotBlank(message = "fileId 不能为空")
    private String fileId;
    /** 原始文件名（≤200 字符） */
    @NotBlank(message = "fileName 不能为空")
    @Size(max = 200, message = "文件名不能超过 200 字符")
    private String fileName;
    /** 文件大小（字节；参考图 ≤5MB，源文件 ≤100MB） */
    @NotNull(message = "fileSize 不能为空")
    private Long fileSize;
    /** 文件类型扩展名（白名单：psd/ai/jpg/png/pdf/webp） */
    @NotBlank(message = "fileType 不能为空")
    @Pattern(regexp = "^(psd|ai|jpg|png|pdf|webp)$", message = "文件格式不在白名单内")
    private String fileType;
    /** 版本说明（1~200 字符） */
    @NotBlank(message = "versionNote 不能为空")
    @Size(min = 1, max = 200, message = "版本说明长度 1~200 字符")
    private String versionNote;
    @NotNull(message = "version 不能为空")
    private Integer version;
}
