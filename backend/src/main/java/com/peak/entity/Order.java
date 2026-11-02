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

/**
 * 订单主表实体 — schema.sql 第 3 张表 `orders`（25 字段）
 *
 * <p>核心特性：乐观锁 version + 逻辑删除 deleted + 归档 archived
 * 状态流转由 Service 层驱动，本 Entity 不包含任何业务逻辑。</p>
 */
@Data
@TableName("orders")
public class Order {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号（业务唯一，格式 ORD-YYYY-NNNNN） */
    private String orderNo;

    /** 客户名称 */
    private String customerName;

    /** 联系人姓名 */
    private String contactName;

    /** 联系电话（详情页完整，列表页脱敏 138****1234） */
    private String contactPhone;

    /** 物料类型 — MaterialType 枚举值：POSTER/FLYER/BUSINESS_CARD/BROCHURE/BANNER/OTHER */
    private String materialType;

    /** 数量（1~99999 正整数） */
    private Integer quantity;

    /** 规格说明（5~200 字符） */
    private String specification;

    /** 交付日期（DATE 类型，早于今天 + 365 天） */
    private LocalDate deliveryDate;

    /** 备注说明（选填，≤500 字符） */
    private String remark;

    /** 订单状态 — OrderStatus 枚举值：CREATED/ASSIGNED/DESIGNING/WAIT_APPROVE/APPROVED/DELIVERED/CANCELLED */
    private String status;

    /** 设计负责人ID（FK → users.id，未分派时为 null） */
    private Long designerId;

    /** 创建人ID（FK → users.id，客户经理 AM） */
    private Long createdBy;

    /** 分派人ID（FK → users.id，业务负责人 BL；未分派时为 null） */
    private Long assignedBy;

    /** 分派时间 */
    private LocalDateTime assignedAt;

    /** 定稿人ID（FK → users.id，业务负责人 BL；未定稿时为 null） */
    private Long approvedBy;

    /** 定稿时间 */
    private LocalDateTime approvedAt;

    /** 定稿版本ID（FK → design_versions.id；未定稿时为 null） */
    private Long finalVersionId;

    /** 交付人ID（FK → users.id，业务负责人 BL；未交付时为 null） */
    private Long deliveredBy;

    /** 交付时间 */
    private LocalDateTime deliveredAt;

    /** 交付备注 */
    private String deliverRemark;

    /** 是否已归档（DB 列 is_archived → Java archived；DELIVERED 自动置 1） */
    @TableField("is_archived")
    private Integer archived;

    /**
     * 乐观锁版本号
     * 更新时自动 version + 1，版本不匹配则影响行数 0 → Service 层抛 ORDER_VERSION_CONFLICT (409)
     * 对应 ARCHITECTURE §5.3 PER-005
     */
    @Version
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     * DB 列 is_deleted → Java deleted（GLOSSARY 规定 Java 布尔字段不用 is 前缀）
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer deleted;
}
