-- D 模块增量迁移：对齐多物料任务模型（order_tasks + uk_task_version）
--
-- 适用：已在旧版「订单级版本」schema 上运行过的 MySQL 库。
-- 新库请直接执行 docs/02-design/schema.sql，无需本脚本。
--
-- 前置条件：
--   1. 已创建 order_tasks 表（多物料任务模型）；
--   2. 已为存量订单生成对应任务记录（本脚本不做伪造 task_id 回填）。
--
-- 变更要点：
--   - design_versions / order_feedbacks / order_reference_files 补 task_id；
--   - design_versions 唯一索引从 uk_order_version(order_id, version_no)
--     切换为 uk_task_version(task_id, version_no)。

ALTER TABLE `design_versions`
  ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '关联物料任务ID（FK -> order_tasks.id）' AFTER `order_id`,
  ADD KEY `idx_task_id` (`task_id`);

ALTER TABLE `order_feedbacks`
  ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '关联物料任务ID（FK -> order_tasks.id）' AFTER `order_id`,
  ADD KEY `idx_task_id` (`task_id`);

ALTER TABLE `order_reference_files`
  ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT '关联物料任务ID（FK -> order_tasks.id）' AFTER `order_id`,
  ADD KEY `idx_task_id` (`task_id`);

-- 唯一约束切换：订单级版本号 → 任务级版本号
ALTER TABLE `design_versions`
  DROP INDEX `uk_order_version`,
  ADD UNIQUE KEY `uk_task_version` (`task_id`, `version_no`) COMMENT '同一任务版本号唯一';

-- 存量数据说明：
--   task_id 必须对应真实存在的 order_tasks.id，不能用 order_id 填充。
--   请按业务映射脚本或人工确认后再执行回填，例如：
--   UPDATE design_versions dv
--     JOIN order_tasks t ON t.order_id = dv.order_id AND t.sort_order = 1
--     SET dv.task_id = t.id
--   WHERE dv.task_id IS NULL;
