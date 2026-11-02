package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /api/v1/auth/login 入参 — API.md §2.1 */
@Data
public class LoginRequest {
    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    private String username;
    /** 登录密码（明文传输，HTTPS 保障） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
