package com.peak.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举 — GLOSSARY.md §3.1 OrderStatus
 *
 * <p>枚举值直接作为 DB orders.status 字段 VARCHAR(32) 存储值。
 * Java 业务逻辑判断用 OrderStatus.WAIT_APPROVE，DB 查询用 code="WAIT_APPROVE"。</p>
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    WAIT_CREATE("WAIT_CREATE", "待登记"),
    CREATED("CREATED", "已登记（待分派）"),
    ASSIGNED("ASSIGNED", "已分派"),
    DESIGNING("DESIGNING", "设计中"),
    WAIT_APPROVE("WAIT_APPROVE", "待审核"),
    REJECTED("REJECTED", "已驳回"),
    APPROVED("APPROVED", "已定稿"),
    DELIVERED("DELIVERED", "已交付"),
    CANCELLED("CANCELLED", "已作废");

    /** DB 存储值（code） */
    private final String code;
    /** 中文显示名 */
    private final String label;

    public static OrderStatus of(String code) {
        for (OrderStatus s : values()) if (s.code.equals(code)) return s;
        return null;
    }
}
