package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * POST /v1/files/reference-files/confirm 入参
 *
 * <p>用于把本地已上传的参考文件回填到 order_reference_files：
 * A 模块创建订单时只写入 file_id，本接口按 orderId + fileId 更新 URL / 文件名 / 文件大小。</p>
 */
@Data
public class ReferenceFileConfirmRequest {

    /** 关联订单 ID */
    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    /** presign / upload 返回的文件 ID */
    @NotBlank(message = "fileId 不能为空")
    private String fileId;

    /** 任务 ID（多物料占位；为空时默认取 orderId） */
    private Long taskId;
}
