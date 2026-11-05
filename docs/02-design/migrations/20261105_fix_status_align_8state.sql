-- ============================================================================
-- 20261105_fix_status_align_8state.sql
-- 项目：ad-material-order-system / MySQL InnoDB / 库 amoms
-- 用途：将种子数据写入的 4 态值（REVIEWING / FINISHED）对齐为后端
--       OrderStatus 枚举 8 态（WAIT_APPROVE / APPROVED / DELIVERED）。
--       修复「定稿审核查不到 WAIT_APPROVE」「归档查询查不到 DELIVERED」。
-- 影响：仅 UPDATE 状态值，不改表结构；幂等（重复执行无副作用）。
-- 执行前建议备份：
--   mysqldump -uroot -p --single-transaction amoms > amoms_backup_20261105_fix.sql
-- ============================================================================

USE `amoms`;

START TRANSACTION;

-- 1. 任务表：REVIEWING → WAIT_APPROVE（待定稿，id 7~12）
UPDATE order_tasks SET status = 'WAIT_APPROVE' WHERE status = 'REVIEWING';

-- 2. 任务表：FINISHED → APPROVED（已定稿，id 13~15，所属订单将置为 DELIVERED）
UPDATE order_tasks SET status = 'APPROVED' WHERE status = 'FINISHED';

-- 3. 订单表：REVIEWING → WAIT_APPROVE（id 5,6,7,10）
UPDATE orders SET status = 'WAIT_APPROVE' WHERE status = 'REVIEWING';

-- 4. 订单表：FINISHED → DELIVERED，并补 is_archived = 1（id 8,9）
UPDATE orders SET status = 'DELIVERED', is_archived = 1 WHERE status = 'FINISHED';

-- 5. 操作日志同步：审核动作的 before/after 值
UPDATE order_operation_logs SET before_value = 'WAIT_APPROVE'
 WHERE operation_type = 'TASK_APPROVE'   AND before_value = 'REVIEWING';
UPDATE order_operation_logs SET after_value = 'APPROVED'
 WHERE operation_type = 'TASK_APPROVE'   AND after_value  = 'FINISHED';
UPDATE order_operation_logs SET before_value = 'DELIVERED', after_value = 'DELIVERED'
 WHERE operation_type = 'ORDER_DELIVER'  AND before_value = 'FINISHED';

COMMIT;

-- ============================================================================
-- 自检（期望全部 0 行 / 指定行数）
-- ============================================================================
-- 5.1 残留 4 态值（期望 0 行）
SELECT 'orders' AS 表, status, COUNT(*) AS cnt FROM orders
 WHERE status IN ('REVIEWING', 'FINISHED')
UNION ALL
SELECT 'order_tasks', status, COUNT(*) FROM order_tasks
 WHERE status IN ('REVIEWING', 'FINISHED');

-- 5.2 新状态分布（期望：WAIT_APPROVE 订单 4 / DELIVERED 订单 2）
SELECT status, COUNT(*) AS cnt, GROUP_CONCAT(id ORDER BY id) AS ids
FROM orders GROUP BY status;
SELECT status, COUNT(*) AS cnt, GROUP_CONCAT(id ORDER BY id) AS ids
FROM order_tasks GROUP BY status;

-- 5.3 定稿审核页数据源（GET /audit/pending 同条件，期望 6 行）
SELECT id, order_id, task_no, status FROM order_tasks WHERE status = 'WAIT_APPROVE';

-- 5.4 归档查询页数据源（GET /archive/orders 同条件，期望 2 行：id 8,9）
SELECT id, order_no, status, is_archived FROM orders
 WHERE is_archived = 1 AND status = 'DELIVERED';
