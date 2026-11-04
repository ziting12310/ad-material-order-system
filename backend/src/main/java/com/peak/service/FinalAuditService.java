package com.peak.service;

import com.peak.dto.ArchiveOrderDTO;
import com.peak.dto.FinalAuditDTO;
import com.peak.vo.ArchiveOrderResponseVO;
import com.peak.vo.FinalAuditResponseVO;
import com.peak.vo.OrderAuditInfoVO;

/**
 * 定稿审核与交付归档服务接口 — C 模块 M5 + M6
 *
 * <p>职责边界：
 *  - 定稿审核（M5）：确认定稿 / 驳回定稿
 *  - 交付归档（M6）：订单交付归档，归档后订单只读
 *  - 配套查询：订单定稿归档信息查询
 *
 * <p>不越界：不操作文件上传、设计版本、客户反馈（D 模块）的主流程，
 * 仅在驳回定稿时复用已有 order_feedbacks 表记录驳回意见（与 A 模块 rejectDesign 保持一致模式）。
 * 不修改枚举类，不修改 A/B/D 模块已有代码。</p>
 */
public interface FinalAuditService {

    /**
     * 确认定稿 — WAIT_APPROVE → APPROVED
     * <p>权限：业务负责人(BL) / 客户经理(AM)
     * <p>前置校验：订单存在、未归档、状态为待定稿、已有设计版本、乐观锁版本匹配
     *
     * @param orderId    订单ID
     * @param dto        审核请求（version + remark）
     * @param operatorId 操作人ID
     * @return 定稿响应（含定稿人、定稿时间、定稿版本）
     */
    FinalAuditResponseVO confirmFinal(Long orderId, FinalAuditDTO dto, Long operatorId);

    /**
     * 驳回定稿 — WAIT_APPROVE → DESIGNING
     * <p>权限：业务负责人(BL) / 客户经理(AM)
     * <p>前置校验：订单存在、未归档、状态为待定稿、乐观锁版本匹配
     * <p>驳回后订单状态回退为 DESIGNING，设计师可继续上传新版本
     *
     * @param orderId    订单ID
     * @param dto        驳回请求（version + remark）
     * @param operatorId 操作人ID
     * @return 驳回响应（含当前状态、版本号）
     */
    FinalAuditResponseVO rejectFinal(Long orderId, FinalAuditDTO dto, Long operatorId);

    /**
     * 订单交付归档 — APPROVED → DELIVERED + archived=1
     * <p>权限：仅业务负责人(BL)
     * <p>前置校验：订单存在、未归档、状态为已定稿、乐观锁版本匹配
     * <p>归档后订单变为只读，所有修改操作（定稿、驳回、上传版本、提交意见）应被拒绝
     *
     * @param orderId    订单ID
     * @param dto        归档请求（version + remark）
     * @param operatorId 操作人ID
     * @return 归档响应（含归档人、归档时间、只读标记）
     */
    ArchiveOrderResponseVO archiveOrder(Long orderId, ArchiveOrderDTO dto, Long operatorId);

    /**
     * 查询订单定稿归档信息
     * <p>用于订单详情页展示定稿记录、归档记录
     *
     * @param orderId 订单ID
     * @return 定稿归档信息 VO
     */
    OrderAuditInfoVO getAuditInfo(Long orderId);
}
