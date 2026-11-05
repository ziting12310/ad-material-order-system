package com.peak.controller;

import com.peak.common.context.UserContext;
import com.peak.dto.DesignVersionCreateRequest;
import com.peak.service.DesignVersionService;
import com.peak.vo.DesignVersionDiffVO;
import com.peak.vo.DesignVersionResponseVO;
import com.peak.vo.DesignVersionVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        Long uploaderId = UserContext.getUserId();
        return ResultVO.success(designVersionService.uploadDesignVersion(dto, uploaderId));
    }

    /**
     * 版本历史 — GET /api/v1/design-versions?orderId=1&taskId=1
     * <p>至少传 orderId / taskId 之一；结果按版本号数字升序（V2 在 V10 前）。</p>
     */
    @GetMapping
    @Operation(summary = "版本历史列表")
    public ResultVO<List<DesignVersionVO>> listVersions(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long taskId,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String userRole = UserContext.getRole();
        return ResultVO.success(designVersionService.listVersions(orderId, taskId, userId, userRole));
    }

    /**
     * 版本对比 — GET /api/v1/design-versions/diff?orderId=1&v1=1&v2=2
     * <p>MVP 返回两个版本的文件 URL，前端并排展示。</p>
     */
    @GetMapping("/diff")
    @Operation(summary = "两个版本对比")
    public ResultVO<DesignVersionDiffVO> diffVersions(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long taskId,
            @RequestParam Integer v1,
            @RequestParam Integer v2,
            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String userRole = UserContext.getRole();
        return ResultVO.success(designVersionService.diffVersions(orderId, taskId, v1, v2, userId, userRole));
    }
}
