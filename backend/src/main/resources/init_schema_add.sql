-- =============================================================================
-- 广告物料订单管理系统 — 增量建表脚本（仅新增，不删除/不改动原有表结构）
-- 数据库：amoms    版本：MySQL 8.0+    引擎：InnoDB    字符集：utf8mb4
-- 生成依据：com.peak.entity 实体定义 + docs/02-design/DATABASE.md 命名规范
-- 执行方式：由开发者在 Navicat 中手动执行，脚本本身不自动运行
--
-- 修复内容：
--   1. 新增 order_tasks（订单物料任务表）      —— 代码已使用，库中缺失
--   2. 新增 order_operation_logs（订单操作日志表）—— 代码已使用，库中缺失
--   3. design_versions / order_feedbacks / order_reference_files 补 task_id 列
--   4. 补 roles 角色字典初始化数据（INSERT IGNORE，可重复执行）
--
-- 注意事项：
--   * MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，第 3 段 ALTER 请在全新环境执行一次；
--     若某列已存在，单独跳过对应 ALTER 语句即可（其余语句可独立执行）。
--   * 全部为增量操作，无 DROP / TRUNCATE。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 订单物料任务表 order_tasks
--    对应实体：com.peak.entity.OrderTask
--    业务说明：一笔订单可包含多个物料任务（多物料模型），任务独立分派、接单、
--             定稿，是设计稿版本与修改意见的归属主体。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_tasks` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '主键ID',
    `order_id`        BIGINT       NOT NULL                           COMMENT '关联订单ID（orders.id）',
    `task_no`         VARCHAR(32)  NOT NULL                           COMMENT '任务编号（订单内唯一，T-001 递增）',
    `material_type`   VARCHAR(32)  NOT NULL                           COMMENT '物料类型（MaterialType枚举：POSTER/FLYER/BUSINESS_CARD/BROCHURE/BANNER/OTHER）',
    `quantity`        INT          NOT NULL                           COMMENT '数量',
    `specification`   VARCHAR(200) NOT NULL                           COMMENT '规格说明',
    `usage_desc`      VARCHAR(200) DEFAULT NULL                       COMMENT '用途说明',
    `deadline`        DATE         NOT NULL                           COMMENT '任务截止日期',
    `remark`          VARCHAR(500) DEFAULT NULL                       COMMENT '任务备注',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'CREATED'         COMMENT '任务状态（OrderStatus枚举）',
    `designer_id`     BIGINT       DEFAULT NULL                       COMMENT '设计负责人ID（未分派时为NULL）',
    `assigned_by`     BIGINT       DEFAULT NULL                       COMMENT '分派人ID（业务负责人）',
    `assigned_at`     DATETIME     DEFAULT NULL                       COMMENT '分派时间',
    `assign_remark`   VARCHAR(200) DEFAULT NULL                       COMMENT '分派备注（分派时填写）',
    `approved_by`     BIGINT       DEFAULT NULL                       COMMENT '定稿人ID（业务负责人）',
    `approved_at`     DATETIME     DEFAULT NULL                       COMMENT '定稿时间',
    `final_version_id` BIGINT      DEFAULT NULL                       COMMENT '定稿版本ID（design_versions.id）',
    `approve_remark`  VARCHAR(200) DEFAULT NULL                       COMMENT '定稿备注',
    `sort_order`      INT          NOT NULL DEFAULT 0                 COMMENT '排序号（物料清单顺序，重建任务时递增）',
    `version`         INT          NOT NULL DEFAULT 0                 COMMENT '乐观锁版本号',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0                 COMMENT '是否删除（0-否 1-是）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_task_no` (`order_id`, `task_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`),
    KEY `idx_designer_id` (`designer_id`),
    KEY `idx_assigned_at` (`assigned_at`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单物料任务表';

-- -----------------------------------------------------------------------------
-- 2. 订单操作日志表 order_operation_logs
--    对应实体：com.peak.entity.OrderOperationLog
--    业务说明：记录订单/任务的状态流转与关键操作，供追溯审计。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_operation_logs` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '主键ID',
    `order_id`       BIGINT       NOT NULL                           COMMENT '关联订单ID（orders.id）',
    `task_id`        BIGINT       DEFAULT NULL                       COMMENT '关联任务ID（order_tasks.id；订单级操作为NULL）',
    `operation_type` VARCHAR(32)  NOT NULL                           COMMENT '操作类型（OperationType枚举）',
    `description`    VARCHAR(500) DEFAULT NULL                       COMMENT '操作描述',
    `before_value`   VARCHAR(500) DEFAULT NULL                       COMMENT '变更前值',
    `after_value`    VARCHAR(500) DEFAULT NULL                       COMMENT '变更后值',
    `operator_id`    BIGINT       NOT NULL                           COMMENT '操作人ID（users.id）',
    `operator_name`  VARCHAR(20)  DEFAULT NULL                       COMMENT '操作人姓名（冗余，便于日志直读）',
    `operator_role`  VARCHAR(32)  DEFAULT NULL                       COMMENT '操作人角色（冗余：AM/BL/DS）',
    `operated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `client_ip`      VARCHAR(64)  DEFAULT NULL                       COMMENT '客户端IP（支持 X-Forwarded-For）',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_operated_at` (`operated_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单操作日志表';

-- -----------------------------------------------------------------------------
-- 3. 三张既有表补 task_id（多物料模型关联字段）
--    存量数据保持 NULL，新写入数据由代码赋值（DesignVersion/OrderFeedback/
--    OrderReferenceFile 三个实体均已有 taskId 字段，库中缺失会导致查询/插入失败）
-- -----------------------------------------------------------------------------
ALTER TABLE `design_versions`
    ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '关联物料任务ID（order_tasks.id；存量数据为NULL）' AFTER `order_id`,
    ADD KEY `idx_task_id` (`task_id`);

ALTER TABLE `order_feedbacks`
    ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '关联物料任务ID（order_tasks.id；存量数据为NULL）' AFTER `order_id`,
    ADD KEY `idx_task_id` (`task_id`);

ALTER TABLE `order_reference_files`
    ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '关联物料任务ID（order_tasks.id；存量数据为NULL）' AFTER `order_id`,
    ADD KEY `idx_task_id` (`task_id`);

-- -----------------------------------------------------------------------------
-- 4. 角色字典初始化数据（对应 DATABASE.md §8.1）
--    INSERT IGNORE：按 role_code 唯一键去重，可重复执行，不会覆盖已有数据
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `roles` (`id`, `role_code`, `role_name`, `description`) VALUES
    (1, 'GUEST', '访客',       '未登录用户，无任何操作权限'),
    (2, 'AM',    '客户经理',   '订单发起人，负责客户对接与订单登记'),
    (3, 'BL',    '业务负责人', '订单管理者，负责分派、审核、交付'),
    (4, 'DS',    '设计制作人员', '设计执行者，负责设计稿上传与迭代');

-- -----------------------------------------------------------------------------
-- 5. 执行后自检（可选，仅查询，不修改数据）
-- -----------------------------------------------------------------------------
-- SHOW TABLES LIKE 'order_%';
-- DESC `order_tasks`;
-- DESC `order_operation_logs`;
-- DESC `design_versions`;
-- DESC `order_feedbacks`;
-- DESC `order_reference_files`;
-- SELECT id, role_code, role_name FROM `roles` ORDER BY id;
