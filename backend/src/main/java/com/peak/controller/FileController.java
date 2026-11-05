package com.peak.controller;

import com.peak.common.context.UserContext;
import com.peak.dto.PresignRequest;
import com.peak.dto.ReferenceFileConfirmRequest;
import com.peak.service.FileService;
import com.peak.vo.PresignResponseVO;
import com.peak.vo.ReferenceFileVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 文件控制器 — 本地文件系统模拟 OSS
 *
 * <p>BaseURL：/v1/files
 * 流程：POST presign 获取 fileId/uploadUrl → POST upload 上传文件 →
 * GET /{fileId} 读取（需要 JWT）→ POST reference-files/confirm 回填参考文件元数据。</p>
 */
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Tag(name = "文件", description = "presign / 本地上传 / 文件读取")
public class FileController {

    private final FileService fileService;

    @PostMapping("/presign")
    @Operation(summary = "生成上传凭证（本地模拟 OSS）")
    public ResultVO<PresignResponseVO> presign(@Valid @RequestBody PresignRequest dto) {
        return ResultVO.success(fileService.presign(dto));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件到本地存储；传 orderId 时自动回填参考文件")
    public ResultVO<PresignResponseVO> upload(
            @RequestParam("fileId") String fileId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long taskId,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(fileService.upload(fileId, file, orderId, taskId, userId));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "读取文件（需要 JWT）")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        Resource resource = fileService.load(fileId);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(fileId, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @PostMapping("/reference-files/confirm")
    @Operation(summary = "回填参考文件 URL / 文件名 / 大小")
    public ResultVO<ReferenceFileVO> confirmReferenceFile(
            @Valid @RequestBody ReferenceFileConfirmRequest dto,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(fileService.confirmReferenceFile(dto, userId));
    }
}
