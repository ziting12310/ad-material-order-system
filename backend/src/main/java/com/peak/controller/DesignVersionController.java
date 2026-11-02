package com.peak.controller;

import com.peak.dto.DesignVersionCreateRequest;
import com.peak.service.DesignVersionService;
import com.peak.vo.DesignVersionResponseVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设计稿版本控制器 — API.md §4.1
 *
 * <p>BaseURL：/api/v1/design-versions
 * 职责：上传设计稿（首版 V1 / 迭代新版本 V2、V3... 共用一个接口）
 * 自动计算版本号 + 推进订单状态 DESIGNING → WAIT_APPROVE</p>
 */
@RestController
@RequestMapping("/v1/design-versions")
@RequiredArgsConstructor
@Tag(name = "设计稿版本", description = "上传首版 / 上传新版本（迭代）")
public class DesignVersionController {

    private final DesignVersionService designVersionService;

    /**
     * 上传设计稿 — US-004 首版 / US-006 新版本
     * POST /api/v1/design-versions
     *
     * <p>请求 JSON（首版示例）：
     * <pre>
     * {
     *   "orderId": 1, "fileId": "presign_file_001",
     *   "fileName": "海报_V1.psd", "fileSize": 20971520,
     *   "fileType": "psd", "versionNote": "首版设计，蓝色主色调",
     *   "version": 1
     * }
     * </pre></p>
     *
     * <p>成功响应：
     * <pre>
     * { "code": "SUCCESS", "data": {
     *   "id": 1, "versionNo": "V1", "fileUrl": "https://oss...",
     *   "orderStatus": "WAIT_APPROVE", "orderVersion": 2
     * }}
     * </pre></p>
     */
    @PostMapping
    @Operation(summary = "上传设计稿（首版 / 新版本共用）")
    public ResultVO<DesignVersionResponseVO> uploadDesignVersion(
            @Valid @RequestBody DesignVersionCreateRequest dto,
            HttpServletRequest request) {
        Long uploaderId = (Long) request.getAttribute("userId");
        return ResultVO.success(designVersionService.uploadDesignVersion(dto, uploaderId));
    }
}
