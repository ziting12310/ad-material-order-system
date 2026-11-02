package com.peak.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体 — schema.sql 第 2 张表 `users`
 *
 * <p>字段命名严格遵循 GLOSSARY.md §5 users 表映射：
 * DB snake_case → Java camelCase；布尔字段 Java 不用 is 前缀。</p>
 */
@Data
@TableName("users")
public class User {

    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号（唯一） */
    private String username;

    /** 密码哈希（bcrypt 强度 10 轮；永远不返回前端） */
    private String passwordHash;

    /** 用户姓名 */
    private String name;

    /** 角色编码 — UserRole 枚举值：AM / BL / DS */
    private String role;

    /** 手机号 */
    private String phone;

    /** 账号是否锁定（0-否 1-是；登录失败 5 次锁定 30 分钟） */
    private Integer locked;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间（ON UPDATE CURRENT_TIMESTAMP） */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     * DB 列 is_deleted → Java 字段 deleted（GLOSSARY 规定：Java 布尔字段不用 is 前缀）
     * MP global-config 已配置 logic-delete-field: deleted
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer deleted;
}
