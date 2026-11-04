package com.peak.util;

import com.peak.common.constants.BusinessErrorCode;
import com.peak.common.enums.OrderStatusEnum;
import com.peak.exception.BusinessException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单状态机工具（A 模块）— 固定流转规则校验，纯静态无状态，不依赖 Spring 容器
 *
 * <p>业务流转顺序（固定）：M0底座 → M1登记(CREATED) → M2分派(ASSIGNED) → M4版本意见(REVIEWING) → M5定稿交付(FINISHED)</p>
 *
 * <p>允许的跃迁边（其余一律非法，含跨级跳跃与反向回退）：
 * <pre>
 *   CREATED   →  ASSIGNED
 *   ASSIGNED  →  REVIEWING
 *   REVIEWING →  FINISHED
 * </pre>
 * FINISHED 为唯一终态（无出边）；CREATED 不能直达 FINISHED，REVIEWING 不能回退 ASSIGNED，
 * 同状态重复提交（无自环边）同样拦截。</p>
 *
 * <p><b>新旧状态模型兼容声明</b>：
 * 新状态机只管控【新 4 态订单】（CREATED / ASSIGNED / REVIEWING / FINISHED）；
 * DB 存量旧订单的 status 是旧 7 态字符串（DESIGNING / WAIT_APPROVE / APPROVED / DELIVERED 等），
 * 经 {@link OrderStatusEnum#safeOf} 解析为 null 后本工具类直接放行，不做新状态机校验、不抛异常，
 * 交给原有老逻辑处理。跨状态模型切换需团队对齐后再改造 ServiceImpl，本版本不动老业务代码。</p>
 *
 * <p>使用约定：Service 层在「更新 DB 之前」调用 {@link #transition}，第一个参数直接传
 * DB 原始状态字符串（解析动作内聚在工具类，调用方无需判 null，杜绝 NPE）；
 * 非法跃迁抛 BusinessException（错误码 {@link BusinessErrorCode#ORDER_STATUS_ILLEGAL_TRANSITION}），
 * 由 GlobalExceptionHandler 统一转 422 ResultVO 返回前端。</p>
 *
 * <p>使用示例 1 —— assignOrder 分派（状态跃迁场景，更新 DB 前先校验）：
 * <pre>{@code
 * // ① 更新 DB 前调用：直接传 DB 取出的原始状态字符串
 * //    新 4 态 → 跃迁校验（仅 CREATED → ASSIGNED 放行）；旧 7 态 → 自动放行走老逻辑
 * OrderStateMachineUtil.transition(order.getStatus(), OrderStatusEnum.ASSIGNED);
 * // ② 校验通过后才推进状态、写库
 * order.setStatus(OrderStatusEnum.ASSIGNED.getCode());
 * orderMapper.updateById(order);
 * }</pre></p>
 *
 * <p>使用示例 2 —— updateOrder 编辑（不产生跃迁，仅 CREATED 态可编辑，用状态守卫）：
 * <pre>{@code
 * // ① 守卫当前状态：新 4 态中非 CREATED 抛 422；旧 7 态订单放行走老逻辑
 * OrderStateMachineUtil.requireState(order.getStatus(), OrderStatusEnum.CREATED, "编辑订单");
 * // ② 仅更新业务字段，status 保持不变，写库
 * order.setCustomerName(dto.getCustomerName());
 * orderMapper.updateById(order);
 * }</pre></p>
 */
public final class OrderStateMachineUtil {

    /** 工具类禁止实例化 */
    private OrderStateMachineUtil() {}

    /**
     * 允许的跃迁边集合：key = 当前状态，value = 允许到达的下一状态集合（终态为空集）。
     * EnumMap 保证枚举 key 高效查找；新增流转规则只需在此登记一条边。
     */
    private static final Map<OrderStatusEnum, Set<OrderStatusEnum>> ALLOWED_TRANSITIONS;

    static {
        Map<OrderStatusEnum, Set<OrderStatusEnum>> map = new EnumMap<>(OrderStatusEnum.class);
        map.put(OrderStatusEnum.CREATED,   EnumSet.of(OrderStatusEnum.ASSIGNED));
        map.put(OrderStatusEnum.ASSIGNED,  EnumSet.of(OrderStatusEnum.REVIEWING));
        map.put(OrderStatusEnum.REVIEWING, EnumSet.of(OrderStatusEnum.FINISHED));
        map.put(OrderStatusEnum.FINISHED,  EnumSet.noneOf(OrderStatusEnum.class));   // 终态：无出边
        ALLOWED_TRANSITIONS = Map.copyOf(map);
    }

    /**
     * 状态跃迁校验 — 更新 DB 之前必须调用（新旧状态模型兼容入口）
     *
     * <p>处理逻辑：
     *  ① 先用 {@link OrderStatusEnum#safeOf} 解析 DB 当前状态；
     *  ② 解析结果为 null（旧 7 态字符串）→ 直接 return：不校验、不抛异常，交给原有老逻辑处理；
     *  ③ 解析成功（新 4 态）→ 执行跃迁规则校验：只允许单向顺序流转
     *     CREATED→ASSIGNED→REVIEWING→FINISHED，禁止跳跃、回退，非法抛 422。</p>
     *
     * @param dbCurrentStatus DB orders.status 原始字符串（新 4 态或旧 7 态均可安全传入，方法内部判 null，无 NPE 风险）
     * @param target          目标状态（本次动作要到达的状态，必须传新 4 态枚举常量）
     * @throws BusinessException 非法跃迁（跨级跳跃 / 反向回退 / 终态再流转 / target 为 null），
     *                           错误码 ORDER_STATUS_ILLEGAL_TRANSITION（422）
     */
    public static void transition(String dbCurrentStatus, OrderStatusEnum target) {
        // ① 兼容分支：DB 是旧 7 态字符串（如 DESIGNING / WAIT_APPROVE）→ safeOf 解析为 null
        //    直接放行、不校验、不抛异常 —— 存量旧订单的状态流转交给原有老逻辑处理
        OrderStatusEnum current = OrderStatusEnum.safeOf(dbCurrentStatus);
        if (current == null) {
            return;
        }

        // ② 目标状态守卫：target 为 null 属于调用方代码缺陷（目标必须来自新 4 态枚举常量），
        //    静默放行会造成状态不推进，必须显式抛错
        if (target == null) {
            throw new BusinessException(
                    BusinessErrorCode.ORDER_STATUS_ILLEGAL_TRANSITION.getCode(),
                    "订单状态流转目标状态非法：target=null");
        }

        // ③ 跃迁边校验：仅新 4 态订单受状态机管控，非法跃迁抛 422
        Set<OrderStatusEnum> allowed = ALLOWED_TRANSITIONS.get(current);
        if (!allowed.contains(target)) {
            throw new BusinessException(
                    BusinessErrorCode.ORDER_STATUS_ILLEGAL_TRANSITION.getCode(),
                    "订单状态非法流转：[%s] 不能变更为 [%s]，允许的下一状态：%s",
                    current.getLabel(), target.getLabel(), formatNext(allowed));
        }
    }

    /**
     * 当前状态守卫 — 适用于「不改变状态、仅特定状态下可操作」的场景（如编辑订单仅 CREATED 可用）
     *
     * <p>与 {@link #transition} 同样的兼容语义：旧 7 态订单解析为 null 后直接放行，
     * 前置状态校验交给原有老逻辑（老逻辑自带 checkStatus）。</p>
     *
     * @param dbCurrentStatus DB orders.status 原始字符串（可为旧 7 态，方法内部判 null，无 NPE 风险）
     * @param expected        允许操作的新 4 态状态
     * @param actionLabel     操作名称（用于错误消息，如"编辑订单"）
     * @throws BusinessException 新 4 态订单且当前状态不在允许范围
     */
    public static void requireState(String dbCurrentStatus, OrderStatusEnum expected, String actionLabel) {
        // 兼容分支：旧 7 态订单不做新状态机守卫，交给原有老逻辑处理
        OrderStatusEnum current = OrderStatusEnum.safeOf(dbCurrentStatus);
        if (current == null) {
            return;
        }
        if (current != expected) {
            throw new BusinessException(
                    BusinessErrorCode.ORDER_STATUS_ILLEGAL_TRANSITION.getCode(),
                    "当前订单状态为 [%s]，%s 仅允许在状态 [%s] 下操作",
                    current.getLabel(), actionLabel, expected.getLabel());
        }
    }

    /** 格式化允许的下一状态提示（终态显示"无（终态）"） */
    private static String formatNext(Set<OrderStatusEnum> allowed) {
        if (allowed.isEmpty()) {
            return "无（终态）";
        }
        return allowed.stream()
                .map(OrderStatusEnum::getLabel)
                .collect(Collectors.joining(" / "));
    }
}
