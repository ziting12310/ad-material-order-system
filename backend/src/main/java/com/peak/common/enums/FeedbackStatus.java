package com.peak.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 修改意见状态枚举 — GLOSSARY.md §3.4 */
@Getter
@AllArgsConstructor
public enum FeedbackStatus {
    PENDING("PENDING", "待处理"),
    PROCESSED("PROCESSED", "已处理");

    private final String code;
    private final String label;

    public static FeedbackStatus of(String code) {
        for (FeedbackStatus s : values()) if (s.code.equals(code)) return s;
        return null;
    }
}
