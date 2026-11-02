package com.peak.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 物料类型枚举 — GLOSSARY.md §3.2 */
@Getter
@AllArgsConstructor
public enum MaterialType {
    POSTER("POSTER", "海报"),
    FLYER("FLYER", "宣传单"),
    BUSINESS_CARD("BUSINESS_CARD", "名片"),
    BROCHURE("BROCHURE", "画册"),
    BANNER("BANNER", "喷绘"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

    public static MaterialType of(String code) {
        for (MaterialType t : values()) if (t.code.equals(code)) return t;
        return null;
    }
}
