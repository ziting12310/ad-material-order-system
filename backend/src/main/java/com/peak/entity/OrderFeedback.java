package com.peak.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单修改意见表实体 — schema.sql 第 5 张表 `order_feedbacks`
 *
 * <p>针对某订单某版本设计稿的修改意见。</p>
 */
@Data
@TableName("order_feedbacks")
public class OrderFeedback {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID（FK → orders.id） */
    private Long orderId;

    /** 关联物料任务ID（FK → order_tasks.id；多物料模型下必填） */
    private Long taskId;

    /** 关联设计稿版本ID（FK → design_versions.id） */
    private Long versionId;

    /** 修改意见内容（TEXT，1~500 字符，XSS 清洗） */
    private String content;

    /** 意见状态 — FeedbackStatus 枚举值：PENDING/PROCESSED */
    private String status;

    /** 提交人ID（FK → users.id，客户经理 AM / 业务负责人 BL） */
    private Long submittedBy;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    /** 处理时间（上传新版本后自动写入） */
    private LocalDateTime processedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除标记（DB 列 is_deleted → Java deleted） */
    @TableField("is_deleted")
    @TableLogic
    private Integer deleted;
}
