package com.peak.vo;

import lombok.Data;

/**
 * 工作台任务状态统计 — 与工作台任务列表同口径（同一可见订单范围 + 任务归属 + 有效状态，已排除逻辑删除与作废订单）
 */
@Data
public class OrderTaskStatsVO {

    /** 待分派任务数（CREATED） */
    private long created;

    /** 待确定任务数（ASSIGNED） */
    private long assigned;

    /** 设计中任务数（DESIGNING） */
    private long designing;

    /** 待审核任务数（WAIT_APPROVE） */
    private long waitApprove;

    /** 已驳回任务数（REJECTED） */
    private long rejected;

    /** 已完成任务数（APPROVED） */
    private long approved;
}
