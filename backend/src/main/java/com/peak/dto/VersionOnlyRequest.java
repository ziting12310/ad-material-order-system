package com.peak.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 仅携带 version 的最小请求体 — 用于 AcceptTask / CancelOrder
 *
 * <p>所有乐观锁保护的写接口都需要 version，抽出避免重复定义。</p>
 */
@Data
public class VersionOnlyRequest {
    /** 乐观锁版本号 */
    @NotNull(message = "version 不能为空")
    private Integer version;
}
