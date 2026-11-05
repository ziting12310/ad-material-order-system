-- ============================================================================
-- 广告制作公司宣传物料订单管理系统 - 完整数据库初始化脚本
-- Database : MySQL 8.0+
-- Charset  : utf8mb4 / utf8mb4_unicode_ci
-- Engine   : InnoDB
-- 版本     : v2.0（与 com.peak.entity 实体逐字段对齐）
--
-- 约定：
--   1. 表名以实体 @TableName 为准（操作日志表为 order_operation_logs）；
--   2. 字段名严格按 MyBatis-Plus 默认驼峰映射（users.locked 即实体字段 locked）；
--   3. 布尔语义字段使用 is_ 前缀列（is_archived / is_final / is_deleted），
--      并通过 @TableField 映射到实体属性；
--   4. 除纯字典表 roles 外，所有表均包含 is_deleted（逻辑删除，默认 0）与
--      version（乐观锁，默认 0）；
--   5. 表之间启用 10 条物理外键（清单见 DATABASE.md §5.3），统一
--      ON DELETE RESTRICT / ON UPDATE CASCADE；orders.final_version_id
--      仅逻辑关联，不建物理外键（规避与 design_versions 的循环依赖）；
--   6. 本脚本会 DROP 既有同名表，仅用于空库/测试库初始化，勿在生产库直接执行。
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
  `role_name`   VARCHAR(32)  NOT NULL COMMENT '角色名称（中文）',
  `description` VARCHAR(200)          DEFAULT NULL COMMENT '角色描述',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`) COMMENT '角色编码唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色字典表';

-- ----------------------------------------------------------------------------
-- 2. users - 用户表（role 保持 VARCHAR 角色编码，不引入 role_id 字段/外键）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`        VARCHAR(50)  NOT NULL COMMENT '登录账号（唯一）',
  `password_hash`   VARCHAR(100) NOT NULL COMMENT '密码哈希（bcrypt，强度10）',
  `name`            VARCHAR(20)  NOT NULL COMMENT '用户姓名',
  `role`            VARCHAR(32)  NOT NULL COMMENT '角色编码（AM/BL/DS）',
  `phone`           VARCHAR(11)           DEFAULT NULL COMMENT '手机号',
  `locked`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否锁定（0-否 1-是）；对应实体 User.locked',
  `last_login_at`   DATETIME              DEFAULT NULL COMMENT '最后登录时间',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`) COMMENT '登录账号唯一',
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
  `quantity`          INT           NOT NULL COMMENT '数量（1~99999）',
  `specification`     VARCHAR(200)  NOT NULL COMMENT '规格说明',
  `delivery_date`     DATE          NOT NULL COMMENT '交付日期',
  `remark`            VARCHAR(500)           DEFAULT NULL COMMENT '备注说明',
  `status`            VARCHAR(32)   NOT NULL DEFAULT 'CREATED' COMMENT '订单状态（OrderStatus枚举）',
  `designer_id`       BIGINT                 DEFAULT NULL COMMENT '设计负责人ID',
  `created_by`        BIGINT        NOT NULL COMMENT '创建人ID（AM）',
  `assigned_by`       BIGINT                 DEFAULT NULL COMMENT '分派人ID（BL）',
  `assigned_at`       DATETIME               DEFAULT NULL COMMENT '分派时间',
  `approved_by`       BIGINT                 DEFAULT NULL COMMENT '定稿人ID（BL）',
  `approved_at`       DATETIME               DEFAULT NULL COMMENT '定稿时间',
  `final_version_id`  BIGINT                 DEFAULT NULL COMMENT '定稿版本ID（design_versions.id）',
  `delivered_by`      BIGINT                 DEFAULT NULL COMMENT '交付人ID（BL）',
  `delivered_at`      DATETIME               DEFAULT NULL COMMENT '交付时间',
  `deliver_remark`    VARCHAR(100)           DEFAULT NULL COMMENT '交付备注',
  `is_archived`       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否已归档（0-否 1-是）；对应实体 Order.archived',
  `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`) COMMENT '订单编号唯一',
  KEY `idx_status` (`status`),
  KEY `idx_designer_id` (`designer_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_delivery_date` (`delivery_date`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ----------------------------------------------------------------------------
-- 4. order_tasks - 物料任务表（多物料模型核心表）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_tasks`;
CREATE TABLE `order_tasks` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`          BIGINT        NOT NULL COMMENT '关联订单ID（orders.id）',
  `task_no`           VARCHAR(20)   NOT NULL COMMENT '任务编号（T-001/T-002...）',
  `material_type`     VARCHAR(32)   NOT NULL COMMENT '物料类型（MaterialType枚举）',
  `quantity`          INT           NOT NULL COMMENT '数量（1~99999）',
  `specification`     VARCHAR(200)  NOT NULL COMMENT '规格说明',
  `usage_desc`        VARCHAR(200)           DEFAULT NULL COMMENT '用途说明',
  `deadline`          DATE          NOT NULL COMMENT '任务截止日期（必填）',
  `remark`            VARCHAR(500)           DEFAULT NULL COMMENT '任务备注',
  `status`            VARCHAR(32)   NOT NULL DEFAULT 'CREATED' COMMENT '任务状态（OrderStatus子集）',
  `designer_id`       BIGINT                 DEFAULT NULL COMMENT '设计人员ID（users.id）',
  `assigned_by`       BIGINT                 DEFAULT NULL COMMENT '分派人ID（BL）',
  `assigned_at`       DATETIME               DEFAULT NULL COMMENT '分派时间',
  `assign_remark`     VARCHAR(200)           DEFAULT NULL COMMENT '分派备注',
  `approved_by`       BIGINT                 DEFAULT NULL COMMENT '定稿人ID（BL）',
  `approved_at`       DATETIME               DEFAULT NULL COMMENT '定稿时间',
  `final_version_id`  BIGINT                 DEFAULT NULL COMMENT '定稿版本ID（design_versions.id）',
  `approve_remark`    VARCHAR(200)           DEFAULT NULL COMMENT '定稿/驳回备注',
  `sort_order`        INT           NOT NULL DEFAULT 0 COMMENT '排序序号（同时用于生成 task_no）',
  `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_task_no` (`order_id`, `task_no`) COMMENT '同一订单内任务编号唯一（含软删除记录）',
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_designer_id` (`designer_id`),
  KEY `idx_deadline` (`deadline`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_order_tasks_order_id`    FOREIGN KEY (`order_id`)    REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_tasks_designer_id` FOREIGN KEY (`designer_id`) REFERENCES `users` (`id`)  ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料任务表';

-- ----------------------------------------------------------------------------
-- 5. design_versions - 设计稿版本表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `design_versions`;
CREATE TABLE `design_versions` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`      BIGINT        NOT NULL COMMENT '关联订单ID（orders.id）',
  `task_id`       BIGINT                 DEFAULT NULL COMMENT '关联物料任务ID（order_tasks.id，可为空）',
  `version_no`    VARCHAR(10)   NOT NULL COMMENT '版本号（V1/V2/V3...）',
  `file_id`       VARCHAR(500)  NOT NULL COMMENT '文件业务ID（本地/OSS Key）',
  `file_url`      VARCHAR(500)  NOT NULL COMMENT '文件访问URL',
  `file_name`     VARCHAR(200)  NOT NULL COMMENT '原始文件名',
  `file_size`     BIGINT        NOT NULL COMMENT '文件大小（字节）',
  `file_type`     VARCHAR(20)   NOT NULL COMMENT '文件类型（扩展名：psd/ai/jpg/png/pdf/webp）',
  `version_note`  VARCHAR(200)  NOT NULL COMMENT '版本说明（1~200字符）',
  `is_final`      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否最终版（0-否 1-是）；对应实体 DesignVersion.finalVersion',
  `uploaded_by`   BIGINT        NOT NULL COMMENT '上传人ID（DS）',
  `uploaded_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`       INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_version` (`order_id`, `version_no`) COMMENT '同一订单版本号唯一',
  KEY `idx_order_id` (`order_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_is_final` (`is_final`),
  KEY `idx_uploaded_at` (`uploaded_at`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_design_versions_order_id` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)      ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_design_versions_task_id`  FOREIGN KEY (`task_id`)  REFERENCES `order_tasks` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计稿版本表';

-- ----------------------------------------------------------------------------
-- 6. order_feedbacks - 订单修改意见表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_feedbacks`;
CREATE TABLE `order_feedbacks` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`       BIGINT       NOT NULL COMMENT '关联订单ID（orders.id）',
  `task_id`        BIGINT                 DEFAULT NULL COMMENT '关联物料任务ID（order_tasks.id，可为空）',
  `version_id`     BIGINT       NOT NULL COMMENT '关联设计稿版本ID（design_versions.id）',
  `content`        TEXT         NOT NULL COMMENT '修改意见内容（1~500字符）',
  `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '意见状态（PENDING/PROCESSED）',
  `submitted_by`   BIGINT       NOT NULL COMMENT '提交人ID（AM/BL）',
  `submitted_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `processed_at`   DATETIME              DEFAULT NULL COMMENT '处理时间（上传新版本后回填）',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  KEY `idx_task_status` (`task_id`, `status`) COMMENT '查询任务下未处理意见',
  KEY `idx_order_id` (`order_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_submitted_by` (`submitted_by`),
  KEY `idx_submitted_at` (`submitted_at`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_order_feedbacks_order_id`   FOREIGN KEY (`order_id`)   REFERENCES `orders` (`id`)           ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_feedbacks_task_id`    FOREIGN KEY (`task_id`)    REFERENCES `order_tasks` (`id`)      ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_feedbacks_version_id` FOREIGN KEY (`version_id`) REFERENCES `design_versions` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单修改意见表';

-- ----------------------------------------------------------------------------
-- 7. order_reference_files - 订单参考文件表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_reference_files`;
CREATE TABLE `order_reference_files` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`      BIGINT        NOT NULL COMMENT '关联订单ID（orders.id）',
  `task_id`       BIGINT                 DEFAULT NULL COMMENT '关联物料任务ID（可为空，订单级参考文件）',
  `file_id`       VARCHAR(500)  NOT NULL COMMENT '文件业务ID（本地/OSS Key）',
  `file_url`      VARCHAR(500)  NOT NULL COMMENT '文件访问URL',
  `file_name`     VARCHAR(200)  NOT NULL COMMENT '原始文件名',
  `file_size`     BIGINT        NOT NULL COMMENT '文件大小（字节，参考图≤5MB）',
  `uploaded_by`   BIGINT        NOT NULL COMMENT '上传人ID',
  `uploaded_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`       INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_uploaded_at` (`uploaded_at`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_order_reference_files_order_id` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)      ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_reference_files_task_id`  FOREIGN KEY (`task_id`)  REFERENCES `order_tasks` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单参考文件表';

-- ----------------------------------------------------------------------------
-- 8. order_operation_logs - 订单操作历史表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_operation_logs`;
CREATE TABLE `order_operation_logs` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`          BIGINT        NOT NULL COMMENT '关联订单ID',
  `task_id`           BIGINT                 DEFAULT NULL COMMENT '关联物料任务ID（订单级操作为NULL）',
  `operation_type`    VARCHAR(64)   NOT NULL COMMENT '操作类型（OperationType枚举）',
  `description`       VARCHAR(500)  NOT NULL COMMENT '操作描述',
  `before_value`      TEXT                   DEFAULT NULL COMMENT '变更前值',
  `after_value`       TEXT                   DEFAULT NULL COMMENT '变更后值',
  `operator_id`       BIGINT        NOT NULL COMMENT '操作人ID',
  `operator_name`     VARCHAR(50)   NOT NULL COMMENT '操作人姓名（冗余）',
  `operator_role`     VARCHAR(32)   NOT NULL COMMENT '操作人角色（冗余）',
  `operated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `client_ip`         VARCHAR(50)            DEFAULT NULL COMMENT '客户端IP',
  `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-否 1-是）',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operated_at` (`operated_at`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_order_operation_logs_order_id` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单操作历史表';

-- ============================================================================
-- 初始化数据
-- ============================================================================

-- ---- 角色字典 4 条（users.role 保留 VARCHAR 编码，不建 role_id 外键） ----
INSERT INTO `roles` (`id`, `role_code`, `role_name`, `description`, `created_at`, `updated_at`)
VALUES
  (1, 'GUEST', '访客',         '未登录用户，无任何操作权限',       '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (2, 'AM',    '客户经理',     '订单发起人，负责客户对接与登记',   '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (3, 'BL',    '业务负责人',   '负责分派、审核与交付',             '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (4, 'DS',    '设计制作人员', '设计执行者，负责设计稿上传与迭代', '2026-08-01 09:00:00', '2026-08-01 09:00:00');

-- ---- 测试用户 6 个：AM/BL/DS 各 2 个，密码统一 123456 ----
-- bcrypt 强度 10，$2a 版本；已用 bcrypt.checkpw("123456", hash) 验证通过
INSERT INTO `users`
  (`id`, `username`, `password_hash`, `name`, `role`, `phone`, `locked`, `version`, `is_deleted`)
VALUES
  (1, 'am_zhangsan', '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '张三', 'AM', '13800138001', 0, 0, 0),
  (2, 'am_lisi',     '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '李四', 'AM', '13800138002', 0, 0, 0),
  (3, 'bl_wangwu',   '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '王五', 'BL', '13800138003', 0, 0, 0),
  (4, 'bl_zhaoliu',  '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '赵六', 'BL', '13800138004', 0, 0, 0),
  (5, 'ds_liming',   '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '李明', 'DS', '13800138005', 0, 0, 0),
  (6, 'ds_qianqi',   '$2a$10$15D0uZbVhGSbFH.Txc0t.eXgQg3PO3gCzTIw24omrIsnuQD0VyGUu', '钱七', 'DS', '13800138006', 0, 0, 0);

-- ---- 1 条测试订单：状态 REVIEWING，已分派给 DS 李明（id=5） ----
INSERT INTO `orders`
  (`id`, `order_no`, `customer_name`, `contact_name`, `contact_phone`,
   `material_type`, `quantity`, `specification`, `delivery_date`, `remark`,
   `status`, `designer_id`, `created_by`, `assigned_by`, `assigned_at`,
   `is_archived`, `version`, `created_at`, `updated_at`, `is_deleted`)
VALUES
  (1, 'ORD-2026-00001', '星光传媒', '陈晨', '13800138100',
   'POSTER', 500, 'A1 海报，300g 铜版纸，四色印刷', '2026-12-31', '客户要求蓝色主色调',
   'REVIEWING', 5, 1, 3, '2026-03-04 10:10:00',
   0, 0, '2026-03-04 10:00:00', '2026-03-04 10:10:00', 0);

-- ---- 2 个测试任务：均 REVIEWING（与订单状态聚合一致） ----
INSERT INTO `order_tasks`
  (`id`, `order_id`, `task_no`, `material_type`, `quantity`, `specification`,
   `usage_desc`, `deadline`, `remark`, `status`, `designer_id`, `assigned_by`, `assigned_at`,
   `sort_order`, `version`, `created_at`, `updated_at`, `is_deleted`)
VALUES
  (1, 1, 'T-001', 'POSTER', 300, 'A1 海报，300g 铜版纸，四色印刷',
   '门店橱窗主视觉', '2026-12-20', '优先出首版', 'REVIEWING', 5, 3, '2026-03-04 10:10:00',
   1, 0, '2026-03-04 10:00:00', '2026-03-04 10:10:00', 0),
  (2, 1, 'T-002', 'POSTER', 200, 'A2 海报，200g 铜版纸',
   '店内活动区海报', '2026-12-25', NULL, 'REVIEWING', 5, 3, '2026-03-04 10:10:00',
   2, 0, '2026-03-04 10:00:00', '2026-03-04 10:10:00', 0);

-- ---- 1 条设计稿版本：任务 T-001 的 V1，由 DS 李明上传 ----
INSERT INTO `design_versions`
  (`id`, `order_id`, `task_id`, `version_no`, `file_id`, `file_url`,
   `file_name`, `file_size`, `file_type`, `version_note`, `is_final`,
   `uploaded_by`, `uploaded_at`, `version`, `created_at`, `updated_at`, `is_deleted`)
VALUES
  (1, 1, 1, 'V1', 'file_demo_poster_v1', '/api/v1/files/file_demo_poster_v1',
   'poster_v1.png', 1048576, 'png', '首版设计稿，主视觉 + 文案排版', 0,
   5, '2026-03-04 11:00:00', 0, '2026-03-04 11:00:00', '2026-03-04 11:00:00', 0);

-- ---- 1 条客户意见：AM 张三对 V1 提出修改意见（PENDING） ----
INSERT INTO `order_feedbacks`
  (`id`, `order_id`, `task_id`, `version_id`, `content`, `status`,
   `submitted_by`, `submitted_at`, `processed_at`, `version`, `created_at`, `updated_at`, `is_deleted`)
VALUES
  (1, 1, 1, 1, '标题字号再放大一号，背景主色偏暗，请调整为亮蓝色。', 'PENDING',
   1, '2026-03-04 11:30:00', NULL, 0, '2026-03-04 11:30:00', '2026-03-04 11:30:00', 0);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 脚本结束
-- ============================================================================
