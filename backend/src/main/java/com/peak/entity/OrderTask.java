package com.peak.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("order_tasks")
public class OrderTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String taskNo;

    private String materialType;

    private Integer quantity;

    private String specification;

    private String usageDesc;

    private LocalDate deadline;

    private String remark;

    private String status;

    private Long designerId;

    private Long assignedBy;

    private LocalDateTime assignedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long finalVersionId;

    private String approveRemark;

    private Integer sortOrder;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic
    private Integer deleted;
}
