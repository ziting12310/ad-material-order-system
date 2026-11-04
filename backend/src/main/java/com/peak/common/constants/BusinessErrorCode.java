package com.peak.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举 — 状态机专用（A 模块新增）
 *
 * <p>与 {@link ErrorCode}（String 常量类）并存：状态机新增的错误码以枚举形式管理，
 * 通过 {@code getCode()} 直接传入 BusinessException(String, String)，
 * GlobalExceptionHandler 按 code 原样透传到 ResultVO，无需感知差异。</p>
 *
 * <p>命名规则与 ErrorCode 一致：HTTP_状态码_业务域_具体错误（本错误码对应 422）。</p>
 */
@Getter
@AllArgsConstructor
public enum BusinessErrorCode {

    /**
     * 订单状态非法流转（422 Unprocessable Entity）：
     * 跃迁不在允许边 CREATED→ASSIGNED→REVIEWING→FINISHED 内，
     * 含跨级跳跃（如 CREATED→FINISHED）、反向回退（如 REVIEWING→ASSIGNED）、终态再流转
     */
    ORDER_STATUS_ILLEGAL_TRANSITION("ORDER_STATUS_ILLEGAL_TRANSITION", "订单状态非法流转");

    /** 错误码（ResultVO.code 返回值，命名风格与 ErrorCode 对齐） */
    private final String code;

    /** 中文描述 */
    private final String desc;
}
