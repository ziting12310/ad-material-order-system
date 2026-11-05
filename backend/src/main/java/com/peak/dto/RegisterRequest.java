package com.peak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /api/v1/auth/register 入参 — 自助注册 */
@Data
public class RegisterRequest {

    /** 登录账号（唯一，3~20 位） */
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 20, message = "账号长度 3~20 位")
    private String username;

    /** 明文密码（HTTPS 传输保障），入库前 bcrypt 加密 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少 6 位")
    private String password;

    /** 用户姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名不能超过 20 个字符")
    private String name;

    /** 手机号（11 位） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
