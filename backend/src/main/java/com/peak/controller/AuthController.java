package com.peak.controller;

import com.peak.dto.LoginRequest;
import com.peak.service.AuthService;
import com.peak.vo.LoginResponseVO;
import com.peak.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 — API.md §2.1 / §2.2
 *
 * <p>BaseURL：/api/v1/auth（yml context-path=/api + 本类 @RequestMapping("/v1") + "/auth"）
 * 职责：登录。文件直传 presign 放在 FileController（不在本类）。</p>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录 / JWT 签发")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * POST /api/v1/auth/login
     *
     * <p>请求 JSON：
     * <pre>{"username": "am_demo", "password": "123456"}</pre></p>
     * <p>成功响应 JSON：
     * <pre>
     * {
     *   "code": "SUCCESS",
     *   "message": "操作成功",
     *   "data": {
     *     "token": "eyJhbGciOiJIUzI1NiIs...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 7200,
     *     "user": { "id": 1, "username": "am_demo", "name": "张三", "role": "AM", "phone": "138****1234" }
     *   },
     *   "traceId": "abc123def456"
     * }
     * </pre></p>
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "账号密码登录，返回 JWT Token + 用户信息")
    public ResultVO<LoginResponseVO> login(@Valid @RequestBody LoginRequest dto) {
        // Controller 仅做三件事：接收（@Valid @RequestBody）→ 调用 Service → 返回 ResultVO
        // 不捕获异常，GlobalExceptionHandler 统一处理 BusinessException
        return ResultVO.success(authService.login(dto));
    }
}
