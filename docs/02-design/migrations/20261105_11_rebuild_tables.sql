-- ============================================================================
-- 20261105_11_rebuild_tables.sql
-- 用途：空库/测试库全量重建 8 张表 + 10 条物理外键（一次跑完）
-- 警告：会 DROP 同名表，仅数据已清空时使用
-- 表创建顺序（被引用主表在前）：roles → users → orders → order_tasks
--                                → design_versions → order_feedbacks
--                                → order_reference_files → order_operation_logs
-- ============================================================================
USE `amoms`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `order_operation_logs`;
DROP TABLE IF EXISTS `order_reference_files`;
DROP TABLE IF EXISTS `order_feedbacks`;
DROP TABLE IF EXISTS `design_versions`;
DROP TABLE IF EXISTS `order_tasks`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `roles`;

-- 1. roles（无外键；无 is_deleted / version）
CREATE TABLE `roles` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code`   VARCHAR(32)  NOT NULL COMMENT '角色编码（AM/BL/DS/GUEST）',
  `role_name`   VARCHAR(32)  NOT NULL COMMENT '角色名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色字典表';

-- 2. users（被 order_tasks.designer_id 引用）
CREATE TABLE `users` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`      VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password_hash` VARCHAR(100) NOT NULL COMMENT '密码哈希（bcrypt）',
  `name`          VARCHAR(20)  NOT NULL COMMENT '用户姓名',
  `role`          VARCHAR(32)  NOT NULL COMMENT '角色编码（AM/BL/DS）',
  `phone`         VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
  `last_login_at` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  `locked`        TINYINT(1)   DEFAULT 0 COMMENT '是否锁定（0-否 1-是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_phone` (`phone`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3. orders（被 order_tasks / design_versions / order_feedbacks / order_reference_files / order_operation_logs 引用）
--    注意：final_version_id 仅逻辑关联，不建物理外键
CREATE TABLE `orders` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`         VARCHAR(20)  NOT NULL COMMENT '订单编号',
  `customer_name`    VARCHAR(100) NOT NULL COMMENT '客户名称',
  `contact_name`     VARCHAR(20)  NOT NULL COMMENT '联系人',
  `contact_phone`    VARCHAR(11)  NOT NULL COMMENT '联系电话',
  `material_type`    VARCHAR(32)  NOT NULL COMMENT '物料类型',
  `quantity`         INT          NOT NULL COMMENT '数量',
  `specification`    VARCHAR(200) NOT NULL COMMENT '规格说明',
  `delivery_date`    DATE         NOT NULL COMMENT '交付日期',
  `remark`           VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status`           VARCHAR(32)  NOT NULL DEFAULT 'CREATED' COMMENT '订单状态（4 态）',
  `designer_id`      BIGINT       DEFAULT NULL COMMENT '设计负责人ID（逻辑关联 users.id）',
  `created_by`       BIGINT       NOT NULL COMMENT '创建人ID（逻辑关联 users.id）',
  `assigned_by`      BIGINT       DEFAULT NULL COMMENT '分派人ID（逻辑关联 users.id）',
  `assigned_at`      DATETIME     DEFAULT NULL COMMENT '分派时间',
  `approved_by`      BIGINT       DEFAULT NULL COMMENT '定稿人ID（逻辑关联 users.id，允许 NULL）',
  `approved_at`      DATETIME     DEFAULT NULL COMMENT '定稿时间',
  `final_version_id` BIGINT       DEFAULT NULL COMMENT '定稿版本ID（仅逻辑关联，无物理外键）',
  `delivered_by`     BIGINT       DEFAULT NULL COMMENT '交付人ID（逻辑关联 users.id）',
  `delivered_at`     DATETIME     DEFAULT NULL COMMENT '交付时间',
  `deliver_remark`   VARCHAR(100) DEFAULT NULL COMMENT '交付备注',
  `is_archived`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否归档',
  `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_designer_id` (`designer_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_created_at_id` (`created_at`, `id` DESC),
  KEY `idx_assigned_at_id` (`assigned_at`, `id` DESC),
  KEY `idx_delivery_date` (`delivery_date`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- 4. order_tasks（引用 orders / users；被 design_versions / order_feedbacks / order_reference_files 引用）
CREATE TABLE `order_tasks` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`         BIGINT       NOT NULL COMMENT '订单ID',
  `task_no`          VARCHAR(32)  NOT NULL COMMENT '任务编号',
  `material_type`    VARCHAR(32)  NOT NULL COMMENT '物料类型',
  `quantity`         INT          NOT NULL COMMENT '数量',
  `specification`    VARCHAR(200) NOT NULL COMMENT '规格说明',
  `usage_desc`       VARCHAR(200) DEFAULT NULL COMMENT '用途说明',
  `deadline`         DATE         NOT NULL COMMENT '任务截止日期',
  `remark`           VARCHAR(500) DEFAULT NULL COMMENT '任务备注',
  `status`           VARCHAR(32)  NOT NULL DEFAULT 'CREATED' COMMENT '任务状态（4 态）',
  `designer_id`      BIGINT       DEFAULT NULL COMMENT '设计人员ID',
  `assigned_by`      BIGINT       DEFAULT NULL COMMENT '分派人ID',
  `assigned_at`      DATETIME     DEFAULT NULL COMMENT '分派时间',
  `assign_remark`    VARCHAR(200) DEFAULT NULL COMMENT '分派备注',
  `approved_by`      BIGINT       DEFAULT NULL COMMENT '定稿人ID（允许 NULL）',
  `approved_at`      DATETIME     DEFAULT NULL COMMENT '定稿时间',
  `final_version_id` BIGINT       DEFAULT NULL COMMENT '定稿版本ID（仅逻辑关联）',
  `approve_remark`   VARCHAR(200) DEFAULT NULL COMMENT '定稿备注',
  `sort_order`       INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
  `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_task_no` (`order_id`, `task_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_designer_id` (`designer_id`),
  KEY `idx_assigned_at` (`assigned_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料任务表';

-- 5. design_versions（引用 orders / order_tasks；被 order_feedbacks 引用）
CREATE TABLE `design_versions` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`     BIGINT       NOT NULL COMMENT '订单ID',
  `task_id`      BIGINT       DEFAULT NULL COMMENT '物料任务ID（可空）',
  `version_no`   VARCHAR(10)  NOT NULL COMMENT '版本号（V1/V2/V3）',
  `file_id`      VARCHAR(500) NOT NULL COMMENT '文件业务ID',
  `file_url`     VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_name`    VARCHAR(200) NOT NULL COMMENT '原始文件名',
  `file_size`    BIGINT       NOT NULL COMMENT '文件大小（字节）',
  `file_type`    VARCHAR(20)  NOT NULL COMMENT '文件类型',
  `version_note` VARCHAR(200) NOT NULL COMMENT '版本说明',
  `is_final`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否最终版',
  `uploaded_by`  BIGINT       NOT NULL COMMENT '上传人ID（逻辑关联 users.id）',
  `uploaded_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_version` (`order_id`, `version_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_is_final` (`is_final`),
  KEY `idx_uploaded_at` (`uploaded_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计稿版本表';

-- 6. order_feedbacks（引用 orders / order_tasks / design_versions）
CREATE TABLE `order_feedbacks` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`     BIGINT       NOT NULL COMMENT '订单ID',
  `task_id`      BIGINT       DEFAULT NULL COMMENT '物料任务ID（可空）',
  `version_id`   BIGINT       NOT NULL COMMENT '设计稿版本ID',
  `content`      TEXT         NOT NULL COMMENT '修改意见内容',
  `status`       VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '意见状态（PENDING/PROCESSED）',
  `submitted_by` BIGINT       NOT NULL COMMENT '提交人ID（逻辑关联 users.id）',
  `submitted_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `processed_at` DATETIME     DEFAULT NULL COMMENT '处理时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_feedback_status` (`order_id`, `status`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_submitted_by` (`submitted_by`),
  KEY `idx_submitted_at` (`submitted_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单修改意见表';

-- 7. order_reference_files（引用 orders / order_tasks）
CREATE TABLE `order_reference_files` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
  `task_id`     BIGINT       DEFAULT NULL COMMENT '物料任务ID（可空）',
  `file_id`     VARCHAR(500) NOT NULL COMMENT '文件业务ID',
  `file_url`    VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_name`   VARCHAR(200) NOT NULL COMMENT '原始文件名',
  `file_size`   BIGINT       NOT NULL COMMENT '文件大小（字节）',
  `uploaded_by` BIGINT       NOT NULL COMMENT '上传人ID（逻辑关联 users.id）',
  `uploaded_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_uploaded_at` (`uploaded_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单参考文件表';

-- 8. order_operation_logs（仅 order_id 有物理外键，task_id 仅逻辑关联）
CREATE TABLE `order_operation_logs` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`       BIGINT       NOT NULL COMMENT '订单ID',
  `task_id`        BIGINT       DEFAULT NULL COMMENT '物料任务ID（仅逻辑关联）',
  `operation_type` VARCHAR(32)  NOT NULL COMMENT '操作类型',
  `description`    VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `before_value`   VARCHAR(500) DEFAULT NULL COMMENT '变更前值',
  `after_value`    VARCHAR(500) DEFAULT NULL COMMENT '变更后值',
  `operator_id`    BIGINT       NOT NULL COMMENT '操作人ID',
  `operator_name`  VARCHAR(20)  DEFAULT NULL COMMENT '操作人姓名',
  `operator_role`  VARCHAR(32)  DEFAULT NULL COMMENT '操作人角色',
  `operated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `client_ip`      VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_operated_at` (`operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单操作历史表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 10 条物理外键（子表列与被引用主键均为 signed BIGINT，可空列对 NOT NULL 主键合法）
-- ============================================================================
ALTER TABLE `order_tasks`
  ADD CONSTRAINT `fk_order_tasks_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `order_tasks`
  ADD CONSTRAINT `fk_order_tasks_designer_id`
  FOREIGN KEY (`designer_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `design_versions`
  ADD CONSTRAINT `fk_design_versions_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `design_versions`
  ADD CONSTRAINT `fk_design_versions_task_id`
  FOREIGN KEY (`task_id`) REFERENCES `order_tasks` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `order_feedbacks`
  ADD CONSTRAINT `fk_order_feedbacks_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `order_feedbacks`
  ADD CONSTRAINT `fk_order_feedbacks_task_id`
  FOREIGN KEY (`task_id`) REFERENCES `order_tasks` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `order_feedbacks`
  ADD CONSTRAINT `fk_order_feedbacks_version_id`
  FOREIGN KEY (`version_id`) REFERENCES `design_versions` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `order_reference_files`
  ADD CONSTRAINT `fk_order_reference_files_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `order_reference_files`
  ADD CONSTRAINT `fk_order_reference_files_task_id`
  FOREIGN KEY (`task_id`) REFERENCES `order_tasks` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `order_operation_logs`
  ADD CONSTRAINT `fk_order_operation_logs_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 自检：期望 10 / 0
SELECT COUNT(*) AS 外键总数 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='amoms';
SELECT COUNT(*) AS 规则不符数 FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA='amoms' AND (DELETE_RULE<>'RESTRICT' OR UPDATE_RULE<>'CASCADE');
