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
 *  ② 旧 7 态兼容：DB 存量旧状态字符串（如 DESIGNING）直接放行，不做新状态机校验。</p>
 *
 * <p>运行：mvn test -Dtest=com.peak.OrderStateMachineUtilTest</p>
 */
@DisplayName("订单状态机：新4态跃迁校验 + 旧7态兼容放行")
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
    @DisplayName("case4: 旧7态 DESIGNING → 兼容放行，不走新状态机校验")
    void case4_legacyStatus_bypass() {
        // DESIGNING 属于旧 7 态：safeOf 解析为 null → 直接放行不校验，
        // 哪怕 DESIGNING→ASSIGNED 不在新状态机允许边内也不拦截，交给原有老逻辑处理
        assertDoesNotThrow(() ->
                OrderStateMachineUtil.transition("DESIGNING", OrderStatusEnum.ASSIGNED),
                "旧 7 态订单应直接放行，不做新状态机校验");
    }
}
