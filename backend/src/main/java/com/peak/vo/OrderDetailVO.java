package com.peak.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO — API.md §3.3
 * 包含订单主信息 + 参考文件 + 设计稿版本 + 修改意见。
 * passwordHash / is_deleted / created_by 内部字段不返回。
 */
@Data
public class OrderDetailVO {
    private Long id;
    private String orderNo;
    private String customerName;
    private String contactName;
    private String contactPhone;       // 详情页完整显示
    private String materialType;
    private Integer quantity;
    private String specification;
    private LocalDate deliveryDate;
    private String remark;
    private String status;
    private Long designerId;
    private String designerName;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private Boolean isArchived;
    private Integer version;

    private List<ReferenceFileVO> referenceFiles;
    private List<DesignVersionVO> designVersions;
    private List<OrderFeedbackVO> feedbacks;

    /** 物料任务列表 */
    private List<OrderTaskVO> tasks;

    /** 操作历史日志 */
    private List<OperationLogVO> operationLogs;

    /** 任务总数 */
    private Integer taskCount;

    /** 已定稿任务数 */
    private Integer approvedTaskCount;
}
