package com.peak;

import com.peak.common.constants.BusinessErrorCode;
import com.peak.common.enums.OrderStatusEnum;
import com.peak.exception.BusinessException;
import com.peak.util.OrderStateMachineUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单状态机单元测试 — 纯静态方法校验，无需 Spring 上下文 / 数据库
 *
 * <p>覆盖两类行为：
 *  ① 新 4 态订单：只允许单向顺序流转 CREATED→ASSIGNED→REVIEWING→FINISHED，非法跃迁抛 422；
 *  ② 旧状态兼容：命中旧状态白名单（DESIGNING / CANCELLED 等）直接放行；
 *     白名单外的 null / 空串 / 未知值拦截（fail-closed）。</p>
 *
 * <p>运行：mvn test -Dtest=com.peak.OrderStateMachineUtilTest</p>
 */
@DisplayName("订单状态机：新4态跃迁校验 + 旧状态白名单兼容")
public class OrderStateMachineUtilTest {

    @Test
    @DisplayName("case1: CREATED → ASSIGNED 顺序流转，放行")
    void case1_createdToAssigned_pass() {
        assertDoesNotThrow(() ->
                OrderStateMachineUtil.transition("CREATED", OrderStatusEnum.ASSIGNED),
                "CREATED→ASSIGNED 是允许的跃迁边，不应抛异常");
    }

    @Test
    @DisplayName("case2: REVIEWING → ASSIGNED 反向回退，抛 422 非法流转")
    void case2_reviewingToAssigned_rejected() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                OrderStateMachineUtil.transition("REVIEWING", OrderStatusEnum.ASSIGNED));
        // 断言错误码 = 422 非法流转
        assertEquals(BusinessErrorCode.ORDER_STATUS_ILLEGAL_TRANSITION.getCode(), ex.getCode());
        System.out.println("case2 拦截消息: " + ex.getMessage());
    }

    @Test
    @DisplayName("case3: FINISHED → REVIEWING 终态再流转，抛 422 非法流转")
    void case3_finishedToReviewing_rejected() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                OrderStateMachineUtil.transition("FINISHED", OrderStatusEnum.REVIEWING));
        assertEquals(BusinessErrorCode.ORDER_STATUS_ILLEGAL_TRANSITION.getCode(), ex.getCode());
        System.out.println("case3 拦截消息: " + ex.getMessage());
    }

    @Test
    @DisplayName("case4: 旧态 DESIGNING → 白名单放行，不走新状态机校验")
    void case4_legacyStatus_bypass() {
        // DESIGNING 命中旧状态白名单：直接放行不校验，
        // 哪怕 DESIGNING→ASSIGNED 不在新状态机允许边内也不拦截，交给原有老逻辑处理
        assertDoesNotThrow(() ->
                OrderStateMachineUtil.transition("DESIGNING", OrderStatusEnum.ASSIGNED),
                "旧状态订单应直接放行，不做新状态机校验");
    }

    @Test
    @DisplayName("case5: 旧态 CANCELLED / 带空格旧态 → 白名单放行")
    void case5_legacyCancelled_bypass() {
        // CANCELLED 不在新 4 态、但命中旧状态白名单 → 放行
        assertDoesNotThrow(() ->
                OrderStateMachineUtil.transition("CANCELLED", OrderStatusEnum.REVIEWING));
        // safeOf / isLegacy 均做 trim 归一化，带首尾空格也能命中白名单
        assertDoesNotThrow(() ->
                OrderStateMachineUtil.transition("  WAIT_APPROVE  ", OrderStatusEnum.FINISHED));
    }

    @Test
    @DisplayName("case6: 未知状态值 / null → 脏数据拦截（fail-closed），不再静默放行")
    void case6_unknownStatus_rejected() {
        // 修复前：safeOf 返回 null 一律放行 → 脏数据可绕过状态机
        // 修复后：不在旧状态白名单的未知值必须拦截
        assertThrows(BusinessException.class, () ->
                OrderStateMachineUtil.transition("UNKNOWN", OrderStatusEnum.ASSIGNED));
        assertThrows(BusinessException.class, () ->
                OrderStateMachineUtil.transition(null, OrderStatusEnum.ASSIGNED));
        assertThrows(BusinessException.class, () ->
                OrderStateMachineUtil.transition("   ", OrderStatusEnum.ASSIGNED));
    }

    @Test
    @DisplayName("case7: target 为 null → 参数守卫先于兼容分支拦截")
    void case7_nullTarget_rejected() {
        // 旧态订单 + target=null：修复前静默 return，修复后必须抛错
        assertThrows(BusinessException.class, () ->
                OrderStateMachineUtil.transition("DESIGNING", null));
        // 新态订单 + target=null 同样拦截
        assertThrows(BusinessException.class, () ->
                OrderStateMachineUtil.transition("CREATED", null));
    }
}
