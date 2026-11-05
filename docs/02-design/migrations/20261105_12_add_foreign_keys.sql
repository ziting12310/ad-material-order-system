-- ============================================================================
-- 20261105_12_add_foreign_keys.sql
-- 用途：当前库（表已存在、数据已清空）上重建 10 条物理外键
-- 前置：先执行 20261105_10_drop_all_foreign_keys.sql
-- 规则：ON DELETE RESTRICT / ON UPDATE CASCADE
-- ============================================================================
USE `amoms`;

-- ① order_tasks.order_id -> orders.id
ALTER TABLE `order_tasks`
  ADD CONSTRAINT `fk_order_tasks_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ② order_tasks.designer_id -> users.id（可空）
ALTER TABLE `order_tasks`
  ADD CONSTRAINT `fk_order_tasks_designer_id`
  FOREIGN KEY (`designer_id`) REFERENCES `users` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ③ design_versions.order_id -> orders.id
ALTER TABLE `design_versions`
  ADD CONSTRAINT `fk_design_versions_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ④ design_versions.task_id -> order_tasks.id（可空）
ALTER TABLE `design_versions`
  ADD CONSTRAINT `fk_design_versions_task_id`
  FOREIGN KEY (`task_id`) REFERENCES `order_tasks` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ⑤ order_feedbacks.order_id -> orders.id
ALTER TABLE `order_feedbacks`
  ADD CONSTRAINT `fk_order_feedbacks_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ⑥ order_feedbacks.task_id -> order_tasks.id（可空）
ALTER TABLE `order_feedbacks`
  ADD CONSTRAINT `fk_order_feedbacks_task_id`
  FOREIGN KEY (`task_id`) REFERENCES `order_tasks` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ⑦ order_feedbacks.version_id -> design_versions.id
ALTER TABLE `order_feedbacks`
  ADD CONSTRAINT `fk_order_feedbacks_version_id`
  FOREIGN KEY (`version_id`) REFERENCES `design_versions` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ⑧ order_reference_files.order_id -> orders.id
ALTER TABLE `order_reference_files`
  ADD CONSTRAINT `fk_order_reference_files_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ⑨ order_reference_files.task_id -> order_tasks.id（可空）
ALTER TABLE `order_reference_files`
  ADD CONSTRAINT `fk_order_reference_files_task_id`
  FOREIGN KEY (`task_id`) REFERENCES `order_tasks` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- ⑩ order_operation_logs.order_id -> orders.id
ALTER TABLE `order_operation_logs`
  ADD CONSTRAINT `fk_order_operation_logs_order_id`
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
  ON DELETE RESTRICT ON UPDATE CASCADE;

-- 自检：期望 10；期望 10 条全部 RESTRICT / CASCADE
SELECT COUNT(*) AS 外键总数
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'amoms';

SELECT COUNT(*) AS 规则不符数
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'amoms'
  AND (DELETE_RULE <> 'RESTRICT' OR UPDATE_RULE <> 'CASCADE');
