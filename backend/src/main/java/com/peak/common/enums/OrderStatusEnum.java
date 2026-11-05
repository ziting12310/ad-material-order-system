package com.peak.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * 订单状态枚举（A 模块固定流转版）— M1登记 → M2分派 → M4版本意见 → M5定稿交付
 *
 * <p>业务流转顺序（固定）：M0底座 → M1(CREATED) → M2(ASSIGNED) → M4(REVIEWING) → M5(FINISHED)。
 * 允许的跃迁边与校验逻辑见 {@link com.peak.util.OrderStateMachineUtil}。</p>
 *
 * <p>与同包 {@link OrderStatus}（旧模型，共 8 个值）并存。
 * <b>兼容声明</b>：新状态机只管控【新 4 态订单】；DB 存量旧订单中，
 * 旧态专属值（WAIT_CREATE / DESIGNING / WAIT_APPROVE / APPROVED / DELIVERED / CANCELLED）
 * 经 {@link #isLegacy} 白名单命中后由工具类放行、走原有老逻辑；
 * 白名单外的未知值 / null / 空白视为脏数据，由工具类拦截（fail-closed）。
 * 注意：CREATED / ASSIGNED 为新旧模型同名重叠值，由新 4 态覆盖、受状态机管控，不列入旧状态白名单。
 * 跨状态模型切换需团队对齐后再改造 ServiceImpl，本版本不动老业务代码。</p>
 *
 * <p>枚举值直接作为 DB orders.status 字段 VARCHAR(32) 存储值；
 * Java 业务逻辑判断用 OrderStatusEnum.REVIEWING，DB 查询用 code="REVIEWING"。</p>
 *
 * <p><b>对外 4 态与内部 7 态映射（口径对齐，避免前后端错位）</b>：
 * <pre>
 * 对外 4 态（本枚举）      内部旧态（OrderStatus，7态）      说明
 * --------------------------------------------------------------------------
 * CREATED              ←  WAIT_CREATE / CREATED            登记待分派
 * ASSIGNED             ←  ASSIGNED                         已分派待接单
 * REVIEWING            ←  DESIGNING / WAIT_APPROVE         设计制作 + 送审/意见往返
 * FINISHED             ←  APPROVED / DELIVERED             定稿 → 交付归档
 * （无对外映射）         ←  CANCELLED                       取消态仅内部使用，不对外暴露
 * </pre>
 * 前端统一按 4 态展示与筛选；后端内部流转仍使用 7 态（OrderStatus），
 * 由 {@link #fromInternal(String)} 做投影后再交给状态机校验。</p>
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

    /**
     * 旧状态白名单（存量订单专用）— 仅用于兼容放行判定，不参与新状态机跃迁校验。
     *
     * <p>只收录「旧模型独有」的状态值；CREATED / ASSIGNED 新旧同名，已由新 4 态覆盖，故不在此列。
     * 白名单是「关闭集合」：不在此表且不在新 4 态的值均视为脏数据，由工具类拦截。</p>
     */
    private static final Set<String> LEGACY_ONLY_CODES = Set.of(
            "WAIT_CREATE", "DESIGNING", "WAIT_APPROVE", "APPROVED", "DELIVERED", "CANCELLED");

    /** DB 存储值（code） */
    private final String code;
    /** 中文显示名 */
    private final String label;

    /**
     * 安全解析 DB 存量状态字符串（兼容新旧两套状态模型，禁止 NPE）
     *
     * <p>行为约定：
     *  ① dbStatus 在新 4 态集合内 → 返回对应枚举，走新状态机 transition 校验；
     *  ② dbStatus 不在集合内（旧状态或未知脏数据）→ 返回 null，
     *     是否为「可放行的旧状态」由 {@link #isLegacy} 单独判定，工具类据此决定放行或拦截。</p>
     *
     * <p>入参做 trim 归一化（容忍 DB 值带首尾空格）；null 直接返回 null，本方法自身不抛异常。</p>
     *
     * @param dbStatus DB orders.status 原始字符串
     * @return 新 4 态枚举；旧状态 / 未知值 / null → null（兼容分支，非异常）
     */
    public static OrderStatusEnum safeOf(String dbStatus) {
        if (dbStatus == null) {
            return null;
        }
        String normalized = dbStatus.trim();
        for (OrderStatusEnum s : values()) if (s.code.equals(normalized)) return s;
        return null;
    }

    /**
     * 是否为存量旧状态（白名单判定，关闭集合）
     *
     * <p>仅白名单内的 6 个旧态值返回 true，命中后由 OrderStateMachineUtil 直接放行、不做新状态机校验；
     * null / 空串 / 空白 / 大小写不匹配 / 未知值一律返回 false —— 交由工具类按脏数据拦截，避免 fail-open。</p>
     *
     * @param dbStatus DB orders.status 原始字符串
     * @return true = 可放行的旧状态；false = 非白名单值
     */
    public static boolean isLegacy(String dbStatus) {
        if (dbStatus == null) {
            return false;
        }
        return LEGACY_ONLY_CODES.contains(dbStatus.trim());
    }

    /**
     * 内部 7 态 → 对外 4 态投影（状态口径对齐入口，Service 层接入状态机时调用）
     *
     * <p>映射规则见类注释映射表，简述：
     * WAIT_CREATE/CREATED→CREATED；ASSIGNED→ASSIGNED；DESIGNING/WAIT_APPROVE→REVIEWING；
     * APPROVED/DELIVERED→FINISHED；CANCELLED 及未知值→null（无对外口径）。</p>
     *
     * <p>返回 null 表示该内部状态不参与对外 4 态展示（目前仅 CANCELLED），
     * 调用方应跳过状态机校验，继续由原有 checkStatus 老逻辑兜底，禁止直接解引用。</p>
     *
     * @param internalStatus DB orders.status 内部状态字符串（可为 7 态任意值）
     * @return 对外 4 态枚举；CANCELLED / 未知值 / null → null
     */
    public static OrderStatusEnum fromInternal(String internalStatus) {
        if (internalStatus == null) {
            return null;
        }
        return switch (internalStatus.trim()) {
            case "WAIT_CREATE", "CREATED" -> CREATED;
            case "ASSIGNED" -> ASSIGNED;
            case "DESIGNING", "WAIT_APPROVE" -> REVIEWING;
            case "APPROVED", "DELIVERED" -> FINISHED;
            default -> null;   // CANCELLED：内部取消态，无对外映射
        };
    }
}
