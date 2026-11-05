package com.peak.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 用户列表项 — 仅暴露前端需要的字段，不返回 passwordHash */
@Data
public class UserListItemVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "用户姓名")
    private String name;

    @Schema(description = "角色编码：AM / BL / DS")
    private String role;

    @Schema(description = "手机号")
    private String phone;
}
