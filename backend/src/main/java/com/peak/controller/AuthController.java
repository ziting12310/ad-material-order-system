package com.peak.controller;

import com.peak.common.context.UserContext;
import com.peak.dto.LoginRequest;
import com.peak.dto.RegisterRequest;
import com.peak.service.AuthService;
import com.peak.vo.LoginResponseVO;
import com.peak.vo.ResultVO;
import com.peak.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PostMapping({"/login", "/login/"})
    @Operation(summary = "用户登录", description = "账号密码登录，返回 JWT Token + 用户信息")
    public ResultVO<LoginResponseVO> login(@Valid @RequestBody LoginRequest dto) {
        // Controller 仅做三件事：接收（@Valid @RequestBody）→ 调用 Service → 返回 ResultVO
        // 不捕获异常，GlobalExceptionHandler 统一处理 BusinessException
        return ResultVO.success(authService.login(dto));
    }

    /**
     * 自助注册
     * POST /api/v1/auth/register
     *
     * <p>匿名可访问（WebMvcConfig 放行），注册用户固定为 DS 角色，成功后返回脱敏用户信息，不返回密码。
     * <pre>请求：{"username":"test_user","password":"123456","name":"测试用户","phone":"13800138000"}</pre></p>
     */
    @PostMapping({"/register", "/register/"})
    @Operation(summary = "用户注册", description = "自助注册账号（固定 DS 角色），密码 bcrypt 加密存储")
    public ResultVO<UserInfoVO> register(@Valid @RequestBody RegisterRequest dto) {
        return ResultVO.success("注册成功", authService.register(dto));
    }

    /**
     * 获取当前登录用户信息
     * GET /api/v1/auth/userinfo
     *
     * <p>用途：获取当前登录用户的信息与角色（AM/BL/DS），供前端菜单鉴权、
     * 按钮权限过滤使用；也可用于页面刷新后恢复用户态。</p>
     * <p>userId 由 JwtInterceptor 解析 JWT 后注入 request 属性，前端无需传 id；
     * 未携带 Token 或 Token 失效时由拦截器直接返回 401，不会进入本方法。</p>
     * <p>成功响应：
     * <pre>
     * { "code": "SUCCESS", "message": "操作成功", "data": {
     *   "id": 1, "username": "am_demo", "name": "张三", "role": "AM", "phone": "138****1234"
     * }}
     * </pre></p>
     */
    @GetMapping("/userinfo")
    @Operation(summary = "获取当前登录用户信息", description = "读取 UserContext 中的 userId，返回用户信息与角色，供前端菜单鉴权使用")
    public ResultVO<UserInfoVO> getUserInfo() {
        return ResultVO.success(authService.getUserInfo(UserContext.getUserId()));
    }

    /**
     * 退出登录
     * POST /api/v1/auth/logout
     *
     * <p>把当前请求的 JWT 加入黑名单，直到其自然过期；之后携带该 Token 的请求会返回 401。</p>
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "将当前 Token 加入黑名单使其立即失效")
    public ResultVO<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        authService.logout(token);
        return ResultVO.success(null);
    }
}
