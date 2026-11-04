package com.peak.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计稿版本表实体 — schema.sql 第 4 张表 `design_versions`
 *
 * <p>订单下的设计稿版本迭代记录，V1/V2/V3... 递增；每次 SubmitFeedback → UploadNewVersion 循环产生新版本。</p>
 *
 * <p>文件存储：
 *  - file_id  → OSS 对象 Key（presign 返回，前端直传后回填，业务定位 + 后端签名 URL 用）
 *  - file_url → 完整访问 URL（前端直接展示用）</p>
 *
 * <p>字段命名严格遵循 GLOSSARY.md §5：DB snake_case → Java camelCase；
 * final 是 Java 关键字，DB is_final → Java finalVersion；布尔字段 Java 不用 is 前缀。</p>
 */
@Data
@TableName("design_versions")
public class DesignVersion {

    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID（FK → orders.id） */
    private Long orderId;

    /** 关联物料任务ID（FK -> order_tasks.id：多物料模型下必填） */
    private Long taskId;

    
    /** 版本号（V1/V2/V3... 字符串递增；ServiceImpl.generateNextVersionNo 自动生成） */
    private String versionNo;

    /**
     * OSS 对象 Key
     * presign 接口返回，前端直传 OSS 后作为业务定位标识；
     * 与 file_url（完整访问 URL）互补：file_id 用于后端生成签名 URL，file_url 用于前端直接展示。
     */
    private String fileId;

    /** 文件访问 URL（直传 OSS；数据库仅存 URL，不存二进制） */
    private String fileUrl;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小（字节；参考图 ≤5MB，源文件 ≤100MB） */
    private Long fileSize;

    /** 文件类型扩展名（白名单：psd/ai/jpg/png/pdf/webp） */
    private String fileType;

    /** 版本说明（1~200 字符，描述本版修改内容） */
    private String versionNote;

    /**
     * 是否最终版
     * DB 列 is_final → Java finalVersion（final 是 Java 关键字，改名）
     * 仅 APPROVED 状态下被确认的版本置 1（ServiceImpl.approveFinal 自动标记）
     */
    @TableField("is_final")
    private Integer finalVersion;

    /** 上传人ID（FK → users.id，设计制作人员 DS） */
    private Long uploadedBy;

    /** 上传时间（冗余 = createdAt） */
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
