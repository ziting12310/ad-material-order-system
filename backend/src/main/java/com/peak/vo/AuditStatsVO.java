package com.peak.vo;

import lombok.Data;

/**
 * 审核工作台统计 — C 模块
 */
@Data
public class AuditStatsVO {

    /** 待审核任务数（WAIT_APPROVE） */
    private long pendingCount;

    /** 已定稿任务数（APPROVED + DELIVERED） */
    private long approvedCount;

    /** 驳回次数（按操作日志 TASK_REJECT / ORDER_REJECT 统计） */
    private long rejectedCount;
}
