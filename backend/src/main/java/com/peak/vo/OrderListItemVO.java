package com.peak.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 订单列表项 VO — API.md §3.1；phone 脱敏、不含内部字段 */
@Data
public class OrderListItemVO {
    private Long id;
    private String orderNo;
    private String customerName;
    private String materialType;
    private Integer quantity;
    private String status;
    private LocalDate deliveryDate;
    private String designerName;
    private LocalDateTime createdAt;

    /** 任务总数 */
    private Integer taskCount;

    /** 已定稿任务数 */
    private Integer approvedTaskCount;

    /** 任务列表（批量组装，前端工作台用于扁平化展示任务级字段） */
    private List<OrderTaskVO> tasks;
}
