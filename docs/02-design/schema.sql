-- ============================================================================
-- 广告制作公司宣传物料订单管理系统 - 数据库初始化脚本
-- Database : MySQL 8.0+
-- Charset  : utf8mb4 / utf8mb4_unicode_ci
-- Engine   : InnoDB
-- 版本     : v1.0
-- 说明     : 可重复执行（IF NOT EXISTS + INSERT IGNORE）
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 0. 数据库
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `amoms`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `amoms`;

-- ----------------------------------------------------------------------------
-- 1. roles - 角色字典表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code`   VARCHAR(32)  NOT NULL COMMENT '角色编码（AM/BL/DS/GUEST）',
  `role_name`   VARCHAR(32)  NOT NULL COMMENT '角色名称',
  `description` VARCHAR(200)          DEFAULT NULL COMMENT '角色描述',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色字典表';

-- ----------------------------------------------------------------------------
-- 2. users - 用户表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`        VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password_hash`   VARCHAR(100) NOT NULL COMMENT '密码哈希（bcrypt）',
  `name`            VARCHAR(20)  NOT NULL COMMENT '用户姓名',
  `role`            VARCHAR(32)  NOT NULL COMMENT '角色编码（UserRole枚举：AM/BL/DS）',
  `phone`           VARCHAR(11)           DEFAULT NULL COMMENT '手机号',
  `is_locked`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否锁定（0-否 1-是）',
  `last_login_at`   DATETIME              DEFAULT NULL COMMENT '最后登录时间',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_phone` (`phone`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------------------------------------------------------
-- 3. orders - 订单主表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`          VARCHAR(20)   NOT NULL COMMENT '订单编号（ORD-YYYY-NNNNN）',
  `customer_name`     VARCHAR(100)  NOT NULL COMMENT '客户名称',
  `contact_name`      VARCHAR(20)   NOT NULL COMMENT '联系人姓名',
  `contact_phone`     VARCHAR(11)   NOT NULL COMMENT '联系电话',
  `material_type`     VARCHAR(32)   NOT NULL COMMENT '物料类型（MaterialType枚举）',
  `quantity`          INT           NOT NULL COMMENT '数量',
  `specification`     VARCHAR(200)  NOT NULL COMMENT '规格说明',
  `delivery_date`     DATE          NOT NULL COMMENT '交付日期',
  `remark`            VARCHAR(500)           DEFAULT NULL COMMENT '备注说明',
  `status`            VARCHAR(32)   NOT NULL DEFAULT 'CREATED' COMMENT '订单状态（OrderStatus枚举）',
  `designer_id`       BIGINT                 DEFAULT NULL COMMENT '设计负责人ID',
  `created_by`        BIGINT        NOT NULL COMMENT '创建人ID（客户经理）',
  `assigned_by`       BIGINT                 DEFAULT NULL COMMENT '分派人ID（业务负责人）',
  `assigned_at`       DATETIME               DEFAULT NULL COMMENT '分派时间',
  `approved_by`       BIGINT                 DEFAULT NULL COMMENT '定稿人ID（业务负责人）',
  `approved_at`       DATETIME               DEFAULT NULL COMMENT '定稿时间',
  `final_version_id`  BIGINT                 DEFAULT NULL COMMENT '定稿版本ID',
  `delivered_by`      BIGINT                 DEFAULT NULL COMMENT '交付人ID（业务负责人）',
  `delivered_at`      DATETIME               DEFAULT NULL COMMENT '交付时间',
  `deliver_remark`    VARCHAR(100)           DEFAULT NULL COMMENT '交付备注',
  `is_archived`       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否已归档（0-否 1-是）',
  `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_designer_id` (`designer_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_created_at_id` (`created_at`, `id` DESC) COMMENT 'Cursor游标分页-按创建时间',
  KEY `idx_assigned_at_id` (`assigned_at`, `id` DESC) COMMENT 'Cursor游标分页-按分派时间（设计师）',
  KEY `idx_delivery_date` (`delivery_date`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ----------------------------------------------------------------------------
-- 4. design_versions - 设计稿版本表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `design_versions`;
CREATE TABLE `design_versions` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`      BIGINT        NOT NULL COMMENT '关联订单ID',
  `version_no`    VARCHAR(10)   NOT NULL COMMENT '版本号（V1/V2/V3...）',
  `file_id`       VARCHAR(500)  NOT NULL COMMENT 'OSS对象Key（presign返回，前端直传后回填）',
  `file_url`      VARCHAR(500)  NOT NULL COMMENT '文件访问URL（OSS）',
  `file_name`     VARCHAR(200)  NOT NULL COMMENT '原始文件名',
  `file_size`     BIGINT        NOT NULL COMMENT '文件大小（字节）',
  `file_type`     VARCHAR(20)   NOT NULL COMMENT '文件类型（扩展名）',
  `version_note`  VARCHAR(200)  NOT NULL COMMENT '版本说明',
  `is_final`      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否最终版（0-否 1-是）',
  `uploaded_by`   BIGINT        NOT NULL COMMENT '上传人ID',
  `uploaded_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_version` (`order_id`, `version_no`) COMMENT '同一订单版本号唯一',
  KEY `idx_order_id` (`order_id`),
  KEY `idx_is_final` (`is_final`),
  KEY `idx_uploaded_at` (`uploaded_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计稿版本表';

-- ----------------------------------------------------------------------------
-- 5. order_feedbacks - 订单修改意见表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_feedbacks`;
CREATE TABLE `order_feedbacks` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`       BIGINT       NOT NULL COMMENT '关联订单ID',
  `version_id`     BIGINT       NOT NULL COMMENT '关联设计稿版本ID',
  `content`        TEXT         NOT NULL COMMENT '修改意见内容',
  `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '意见状态（FeedbackStatus枚举：PENDING/PROCESSED）',
  `submitted_by`   BIGINT       NOT NULL COMMENT '提交人ID',
  `submitted_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `processed_at`   DATETIME              DEFAULT NULL COMMENT '处理时间',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  KEY `idx_order_feedback_status` (`order_id`, `status`) COMMENT '按订单+状态查询意见',
  KEY `idx_version_id` (`version_id`),
  KEY `idx_submitted_by` (`submitted_by`),
  KEY `idx_submitted_at` (`submitted_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单修改意见表';

-- ----------------------------------------------------------------------------
-- 6. order_reference_files - 订单参考文件表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_reference_files`;
CREATE TABLE `order_reference_files` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`      BIGINT        NOT NULL COMMENT '关联订单ID',
  `file_id`       VARCHAR(500)  NOT NULL COMMENT 'OSS对象Key（presign返回，前端直传后回填）',
  `file_url`      VARCHAR(500)  NOT NULL COMMENT '文件访问URL（OSS）',
  `file_name`     VARCHAR(200)  NOT NULL COMMENT '原始文件名',
  `file_size`     BIGINT        NOT NULL COMMENT '文件大小（字节）',
  `uploaded_by`   BIGINT        NOT NULL COMMENT '上传人ID',
  `uploaded_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_uploaded_at` (`uploaded_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单参考文件表';

-- ============================================================================
-- 初始化数据
-- ============================================================================

-- ---- 角色字典（4条，INSERT IGNORE 可重复执行） ----
INSERT IGNORE INTO `roles` (`id`, `role_code`, `role_name`, `description`) VALUES
  (1, 'GUEST',  '访客',          '未登录用户，无任何操作权限'),
  (2, 'AM',     '客户经理',      '订单发起人，负责客户对接与订单登记'),
  (3, 'BL',     '业务负责人',    '订单管理者，负责分派、审核、交付'),
  (4, 'DS',     '设计制作人员',  '设计执行者，负责设计稿上传与迭代');

-- ---- 测试用户（密码均为 123456，bcrypt哈希值） ----
-- bcrypt hash for "123456" with 10 rounds: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT IGNORE INTO `users` (`id`, `username`, `password_hash`, `name`, `role`, `phone`, `is_locked`) VALUES
  (1, 'am_zhangsan',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', 'AM', '13800138001', 0),
  (2, 'am_lisi',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', 'AM', '13800138002', 0),
  (3, 'bl_wangwu',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王五', 'BL', '13800138003', 0),
  (4, 'ds_liming',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李明', 'DS', '13800138004', 0),
  (5, 'ds_zhaoliu',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵六', 'DS', '13800138005', 0),
  (6, 'ds_qianqi',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '钱七', 'DS', '13800138006', 0);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 脚本结束
-- ============================================================================
