package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.FeedbackStatus;
import com.peak.common.enums.OrderStatus;
import com.peak.dto.OrderFeedbackCreateRequest;
import com.peak.entity.DesignVersion;
import com.peak.entity.Order;
import com.peak.entity.OrderFeedback;
import com.peak.exception.BusinessException;
import com.peak.mapper.DesignVersionMapper;
import com.peak.mapper.OrderFeedbackMapper;
import com.peak.mapper.OrderMapper;
import com.peak.service.OrderFeedbackService;
import com.peak.vo.OrderFeedbackResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 修改意见服务实现 — API.md §5.1 SubmitFeedback
 *
 * <p>伪代码流程：
 *  ① 查订单 → 不存在 → ORDER_NOT_FOUND 404
 *  ② 状态校验（WAIT_APPROVE 才能提意见）→ 冲突 → ORDER_STATUS_CONFLICT 422
 *  ③ 乐观锁校验 → 冲突 → ORDER_VERSION_CONFLICT 409
 *  ④ XSS 清洗意见内容（StringEscapeUtils.escapeHtml4）
 *  ⑤ 查最新版本 ID（作为 version_id 外键关联）
 *  ⑥ 插入 OrderFeedback（PENDING 状态）+ 更新 Order 状态为 DESIGNING（同事务）
 *  ⑦ 组装 OrderFeedbackResponseVO</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFeedbackServiceImpl implements OrderFeedbackService {

    private final OrderMapper orderMapper;
    private final OrderFeedbackMapper orderFeedbackMapper;
    private final DesignVersionMapper designVersionMapper;   // 修正：注入 DesignVersionMapper

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderFeedbackResponseVO submitFeedback(OrderFeedbackCreateRequest dto, Long submitterId) {
        // ① 查订单
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        // ② 状态校验：仅 WAIT_APPROVE 可提修改意见（定稿后不能再提）
        if (!OrderStatus.WAIT_APPROVE.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,
                    "当前订单状态为 [%s]，提交修改意见需要状态为 [待定稿]",
                    OrderStatus.of(order.getStatus()) != null
                            ? OrderStatus.of(order.getStatus()).getLabel() : order.getStatus());
        }

        // ③ 乐观锁校验
        if (!order.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT,
                    "数据已变更，请刷新后重试（当前版本 %d，请求版本 %d）",
                    order.getVersion(), dto.getVersion());
        }

        // ④ XSS 清洗意见内容（ARCH §5.1 SEC-001）
        String safeContent = StringEscapeUtils.escapeHtml4(dto.getContent());

        // ⑤ 查最新设计稿版本（作为 Feedback 的 version_id 外键）
        DesignVersion latest = designVersionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getOrderId, order.getId())
                        .orderByDesc(DesignVersion::getId)
                        .last("LIMIT 1")
        );
        Long latestVersionId = latest != null ? latest.getId() : null;

        // ⑥ 插入 Feedback
        LocalDateTime now = LocalDateTime.now();
        OrderFeedback feedback = new OrderFeedback();
        feedback.setOrderId(order.getId());
        feedback.setVersionId(latestVersionId);
        feedback.setContent(safeContent);
        feedback.setStatus(FeedbackStatus.PENDING.getCode());
        feedback.setSubmittedBy(submitterId);
        feedback.setSubmittedAt(now);
        orderFeedbackMapper.insert(feedback);

        // ⑦ 订单状态回退为 DESIGNING（触发设计人员继续修改）
        order.setStatus(OrderStatus.DESIGNING.getCode());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_VERSION_CONFLICT, "订单已被他人修改");
        }

        // ⑧ 组装响应
        OrderFeedbackResponseVO resp = new OrderFeedbackResponseVO();
        resp.setId(feedback.getId());
        resp.setContent(safeContent);
        resp.setStatus(feedback.getStatus());
        resp.setVersionId(latestVersionId);
        resp.setSubmittedAt(now);
        resp.setOrderStatus(order.getStatus());
        resp.setOrderVersion(order.getVersion());
        return resp;
    }
}
