package com.peak.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_operation_logs")
public class OrderOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long taskId;

    private String operationType;

    private String description;

    private String beforeValue;

    private String afterValue;

    private Long operatorId;

    private String operatorName;

    private String operatorRole;

    private LocalDateTime operatedAt;

    private String clientIp;
}
