package com.peak.vo;

import lombok.Data;

/** POST /api/v1/auth/login 响应 data — API.md §2.1 */
@Data
public class LoginResponseVO {
    private String token;              // JWT 令牌
    private String tokenType;          // 固定 "Bearer"
    private Long expiresIn;            // 7200（秒）
    private UserInfoVO user;
}
