package com.peak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 — 个人中心自助修改
 *
 * <p>校验链：
 * <ol>
 *   <li>Controller @Valid 触发 Bean Validation（3 个字段必填，长度 6-32）</li>
 *   <li>Service 层校验原密码 BCrypt 匹配</li>
 *   <li>新密码 BCrypt 加密更新到 DB</li>
 * </ol>
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码（BCrypt 校验）", example = "123456")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需在 6-32 位之间")
    @Schema(description = "新密码（6-32 位，BCrypt 加密存储）", example = "newpass123")
    private String newPassword;
}
