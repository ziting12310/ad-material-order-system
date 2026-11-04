package com.peak.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举（A 模块固定流转版）— M1登记 → M2分派 → M4版本意见 → M5定稿交付
 *
 * <p>业务流转顺序（固定）：M0底座 → M1(CREATED) → M2(ASSIGNED) → M4(REVIEWING) → M5(FINISHED)。
 * 允许的跃迁边与校验逻辑见 {@link com.peak.util.OrderStateMachineUtil}。</p>
 *
 * <p>与同包 {@link OrderStatus}（旧全量 7 态：DESIGNING / WAIT_APPROVE / APPROVED / DELIVERED 等）并存。
 * <b>兼容声明</b>：新状态机只管控【新 4 态订单】；DB 存量旧订单的 status 是旧 7 态字符串，
 * 经 {@link #safeOf} 解析为 null 后由工具类放行、走原有老逻辑；
 * 跨状态模型切换需团队对齐后再改造 ServiceImpl，本版本不动老业务代码。</p>
 *
 * <p>枚举值直接作为 DB orders.status 字段 VARCHAR(32) 存储值；
 * Java 业务逻辑判断用 OrderStatusEnum.REVIEWING，DB 查询用 code="REVIEWING"。</p>
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /** M1 — 登记（唯一初始态） */
    CREATED("CREATED", "已登记（待分派）"),
    /** M2 — 分派 */
    ASSIGNED("ASSIGNED", "已分派"),
    /** M4 — 版本意见（设计稿送审） */
    REVIEWING("REVIEWING", "版本意见（审核中）"),
    /** M5 — 定稿交付（唯一终态） */
    FINISHED("FINISHED", "已完成（定稿交付）");

    /** DB 存储值（code） */
    private final String code;
    /** 中文显示名 */
    private final String label;

    /**
     * 安全解析 DB 存量状态字符串（兼容新旧两套状态模型，禁止 NPE）
     *
     * <p>行为约定：
     *  ① dbStatus 在新 4 态集合内 → 返回对应枚举，走新状态机 transition 校验；
     *  ② dbStatus 不在集合内（属于旧 7 态字符串如 DESIGNING / WAIT_APPROVE，或脏数据）→ 返回 null，
     *     上层 OrderStateMachineUtil 判 null 后跳过状态机校验、直接放行，交给原有老逻辑处理。</p>
     *
     * <p>注意：本方法设计上「以 null 表达旧状态」这一兼容分支，调用方必须判 null，
     * 禁止直接解引用返回值。</p>
     *
     * @param dbStatus DB orders.status 原始字符串
     * @return 新 4 态枚举；旧 7 态 / 未知值返回 null（兼容分支，非异常）
     */
    public static OrderStatusEnum safeOf(String dbStatus) {
        for (OrderStatusEnum s : values()) if (s.code.equals(dbStatus)) return s;
        return null;   // 旧 7 态 / 未知值 → null，由上层工具类放行
    }
}
