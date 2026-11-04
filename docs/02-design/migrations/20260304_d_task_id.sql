-- D 模块增量迁移：为三张表补 task_id 占位字段（不新建 tasks 表）
-- 适用：已在旧 schema 上运行的 MySQL 库；新库直接执行 schema.sql 即可。
-- 约定：task_id 可空，业务层为空时默认写入 order_id；唯一索引仍为 uk_order_version(order_id, version_no)。

ALTER TABLE `design_versions`
  ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '任务ID（多物料占位；无 tasks 表，默认=order_id）' AFTER `order_id`,
  ADD KEY `idx_task_id` (`task_id`);

ALTER TABLE `order_feedbacks`
  ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '任务ID（多物料占位；无 tasks 表，默认=order_id）' AFTER `order_id`,
  ADD KEY `idx_task_id` (`task_id`);

ALTER TABLE `order_reference_files`
  ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '任务ID（多物料占位；无 tasks 表，默认=order_id）' AFTER `order_id`,
  ADD KEY `idx_task_id` (`task_id`);

-- 存量数据回填：让按 taskId 查询能命中老数据
UPDATE `design_versions` SET `task_id` = `order_id` WHERE `task_id` IS NULL;
UPDATE `order_feedbacks` SET `task_id` = `order_id` WHERE `task_id` IS NULL;
UPDATE `order_reference_files` SET `task_id` = `order_id` WHERE `task_id` IS NULL;
