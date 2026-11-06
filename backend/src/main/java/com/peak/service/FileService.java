package com.peak.service;

import com.peak.dto.PresignRequest;
import com.peak.dto.ReferenceFileConfirmRequest;
import com.peak.vo.PresignResponseVO;
import com.peak.vo.ReferenceFileVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务 — 本地文件系统模拟 OSS
 *
 * <p>流程：presign 返回 uploadUrl / accessUrl → upload 落盘 → GET 读取文件。
 * 文件 ID 由服务端生成，格式 UUID + 扩展名；不使用客户端文件名作为存储路径。</p>
 */
public interface FileService {

    /** 生成上传凭证（本地模拟，不依赖真实 OSS） */
    PresignResponseVO presign(PresignRequest dto);

    /** 接收上传文件并落盘；传 orderId 时同时回填参考文件元数据 */
    PresignResponseVO upload(String fileId, MultipartFile file, Long orderId, Long taskId, Long userId);

    /** 按 fileId 读取文件（带数据范围校验） */
    Resource load(String fileId, Long userId, String userRole);

    /** 推导文件访问 URL（存库用） */
    String resolveAccessUrl(String fileId);

    /** 解析文件原始文件名（从 .meta 读取，读不到返回 null） */
    String resolveFileName(String fileId);

    /** 回填参考文件元数据到 order_reference_files */
    ReferenceFileVO confirmReferenceFile(ReferenceFileConfirmRequest dto, Long userId);
}
