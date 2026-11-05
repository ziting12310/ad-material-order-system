-- ============================================================================
-- 20261105_10_drop_all_foreign_keys.sql
-- 用途：删除 amoms 库中全部已存在的外键（先在 Navicat 执行本脚本）
-- 说明：只删外键约束，不删表、不删索引、不删数据
-- ============================================================================
USE `amoms`;

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `order_operation_logs`  DROP FOREIGN KEY `fk_order_operation_logs_order_id`;
ALTER TABLE `order_reference_files` DROP FOREIGN KEY `fk_order_reference_files_task_id`;
ALTER TABLE `order_reference_files` DROP FOREIGN KEY `fk_order_reference_files_order_id`;
ALTER TABLE `order_feedbacks`       DROP FOREIGN KEY `fk_order_feedbacks_version_id`;
ALTER TABLE `order_feedbacks`       DROP FOREIGN KEY `fk_order_feedbacks_task_id`;
ALTER TABLE `order_feedbacks`       DROP FOREIGN KEY `fk_order_feedbacks_order_id`;
ALTER TABLE `design_versions`       DROP FOREIGN KEY `fk_design_versions_task_id`;
ALTER TABLE `design_versions`       DROP FOREIGN KEY `fk_design_versions_order_id`;
ALTER TABLE `order_tasks`           DROP FOREIGN KEY `fk_order_tasks_designer_id`;
ALTER TABLE `order_tasks`           DROP FOREIGN KEY `fk_order_tasks_order_id`;

SET FOREIGN_KEY_CHECKS = 1;

-- 自检：期望 0
SELECT COUNT(*) AS 剩余外键数
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'amoms';

-- ----------------------------------------------------------------------------
-- 可选：如果库里还有未知名字的外键，用下面这段动态删除（Navicat 可整段执行）
-- ----------------------------------------------------------------------------
-- SET FOREIGN_KEY_CHECKS = 0;
-- SET @sql = NULL;
-- SELECT GROUP_CONCAT(CONCAT('ALTER TABLE `', TABLE_NAME, '` DROP FOREIGN KEY `', CONSTRAINT_NAME, '`;')
--                     SEPARATOR ' ')
--   INTO @sql
-- FROM information_schema.KEY_COLUMN_USAGE
-- WHERE TABLE_SCHEMA = 'amoms' AND REFERENCED_TABLE_NAME IS NOT NULL;
-- PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
-- SET FOREIGN_KEY_CHECKS = 1;
