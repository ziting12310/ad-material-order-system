package com.peak.service;

import com.peak.dto.OrderFeedbackCreateRequest;
import com.peak.vo.OrderFeedbackResponseVO;

/**
 * 修改意见服务接口 — API.md §5.1 SubmitFeedback
 *
 * <p>职责：意见提交 + 订单状态回退（WAIT_APPROVE → DESIGNING，触发设计人员重新修改）</p>
 */
public interface OrderFeedbackService {

    /**
     * 提交修改意见
     * <p>
     * 伪代码流程：
     *  ① 查订单 → 不存在 → ORDER_NOT_FOUND
     *  ② 状态校验（WAIT_APPROVE 才能提意见）→ 冲突 → ORDER_STATUS_CONFLICT
     *  ③ 乐观锁校验 → 冲突 → ORDER_VERSION_CONFLICT
     *  ④ XSS 清洗意见内容（StringEscapeUtils）
     *  ⑤ 插入 OrderFeedback + 更新 Order 状态为 DESIGNING（同事务）
     *  ⑥ 组装 OrderFeedbackResponseVO（含订单更新后状态 + 版本号）
     * </p>
     */
    OrderFeedbackResponseVO submitFeedback(OrderFeedbackCreateRequest dto, Long submitterId);
}
