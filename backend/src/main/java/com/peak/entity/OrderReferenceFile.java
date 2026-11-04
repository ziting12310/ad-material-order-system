package com.peak.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单参考文件表实体 — schema.sql 第 6 张表 `order_reference_files`
 *
 * <p>订单登记时上传的参考资料文件（图片/PDF）；前端直传 OSS，数据库存储：
 *  - file_id  → OSS 对象 Key（presign 返回，前端直传后回填，业务定位用）
 *  - file_url → 完整访问 URL（前端渲染展示用）</p>
 *
 * <p>字段命名严格遵循 GLOSSARY.md §5：DB snake_case → Java camelCase；
 * 布尔字段 Java 不用 is 前缀（deleted 映射 is_deleted）。</p>
 */
@Data
@TableName("order_reference_files")
public class OrderReferenceFile {

    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID（FK → orders.id） */
    private Long orderId;

    /** 关联物料任务ID（FK -> order_tasks.id：多物料模型下必填） */
    private Long taskId;

   

    /**
     * OSS 对象 Key
     * presign 接口返回，前端直传 OSS 后作为业务定位标识；
     * 与 file_url（完整访问 URL）互补：file_id 用于后端生成签名 URL，file_url 用于前端直接展示。
     */
    private String fileId;

    /** 文件访问 URL（OSS） */
    private String fileUrl;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小（字节；参考图 ≤5MB） */
    private Long fileSize;

    /** 上传人ID（FK → users.id，客户经理 AM） */
    private Long uploadedBy;

    /** 上传时间 */
    private LocalDateTime uploadedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     * DB 列 is_deleted → Java 字段 deleted（GLOSSARY 规定：Java 布尔字段不用 is 前缀）
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer deleted;
}
