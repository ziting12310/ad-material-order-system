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

    /** 是否已归档：订单下全部有效物料均已定稿 */
    private Boolean archived;

    /** 归档状态文案：已归档 / 未归档 */
    private String archiveStatus;

    /** 归档时间：全部物料定稿时取最后一次定稿时间；存在未定稿物料时为 null */
    private LocalDateTime archivedAt;

    /** 任务列表（批量组装，前端工作台用于扁平化展示任务级字段） */
    private List<OrderTaskVO> tasks;
}
