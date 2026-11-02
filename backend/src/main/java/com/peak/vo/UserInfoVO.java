package com.peak.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** 登录响应 user 子对象 — API.md §2.1；phone 脱敏 138****1234 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoVO {
    private Long id;
    private String username;
    private String name;
    private String role;
    private String phone;
}
