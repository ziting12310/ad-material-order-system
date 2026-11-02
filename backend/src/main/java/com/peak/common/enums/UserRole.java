package com.peak.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 用户角色枚举 — GLOSSARY.md §3.3；schema.sql roles 表 4 条字典数据 */
@Getter
@AllArgsConstructor
public enum UserRole {
    GUEST("GUEST", "访客"),
    AM("AM", "客户经理"),
    BL("BL", "业务负责人"),
    DS("DS", "设计制作人员");

    private final String code;
    private final String label;

    public static UserRole of(String code) {
        for (UserRole r : values()) if (r.code.equals(code)) return r;
        return null;
    }
}
