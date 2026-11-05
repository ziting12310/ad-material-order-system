-- ============================================================================
-- 20261105_verify.sql
-- 项目：ad-material-order-system
-- 用途：物理外键 + 种子数据校验（只读，不修改任何数据）
-- 执行：mysql -uroot -p amoms < 20261105_verify.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 外键清单校验（期望 10 条，全部 RESTRICT / CASCADE）
-- ----------------------------------------------------------------------------
SELECT kcu.TABLE_NAME          AS 子表,
       kcu.COLUMN_NAME         AS 外键字段,
       kcu.REFERENCED_TABLE_NAME  AS 主表,
       kcu.REFERENCED_COLUMN_NAME AS 主表字段,
       rc.DELETE_RULE          AS 删除规则,
       rc.UPDATE_RULE          AS 更新规则
FROM information_schema.KEY_COLUMN_USAGE kcu
JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
  ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
 AND rc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
WHERE kcu.CONSTRAINT_SCHEMA = 'amoms'
  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY kcu.TABLE_NAME, kcu.COLUMN_NAME;

-- 数量断言（期望 10）
SELECT COUNT(*) AS fk_count
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'amoms';

-- 规则断言（期望 0 条非 RESTRICT / 非 CASCADE）
SELECT COUNT(*) AS bad_rule_count
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'amoms'
  AND (DELETE_RULE <> 'RESTRICT' OR UPDATE_RULE <> 'CASCADE');

-- ----------------------------------------------------------------------------
-- 2. 外键配套索引校验（每个外键字段都应有索引，且外键列位于索引最左列）
-- ----------------------------------------------------------------------------
SELECT kcu.TABLE_NAME, kcu.COLUMN_NAME,
       IFNULL(MIN(st.INDEX_NAME), 'NO INDEX') AS 命中索引
FROM information_schema.KEY_COLUMN_USAGE kcu
LEFT JOIN information_schema.STATISTICS st
  ON st.TABLE_SCHEMA = kcu.TABLE_SCHEMA
 AND st.TABLE_NAME   = kcu.TABLE_NAME
 AND st.COLUMN_NAME  = kcu.COLUMN_NAME
 AND st.SEQ_IN_INDEX = 1
WHERE kcu.CONSTRAINT_SCHEMA = 'amoms'
  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
GROUP BY kcu.TABLE_NAME, kcu.COLUMN_NAME
ORDER BY kcu.TABLE_NAME, kcu.COLUMN_NAME;

-- ----------------------------------------------------------------------------
-- 3. 完整链路 JOIN（一单一行，查看订单 → 任务 → 版本 → 意见 → 附件 → 日志）
-- ----------------------------------------------------------------------------
SELECT o.order_no,
       o.status                     AS 订单状态,
       u.username                   AS 创建人,
       d.username                   AS 设计师,
       COUNT(DISTINCT t.id)         AS 任务数,
       COUNT(DISTINCT v.id)         AS 版本数,
       COUNT(DISTINCT f.id)         AS 意见数,
       COUNT(DISTINCT r.id)         AS 参考附件数,
       COUNT(DISTINCT l.id)         AS 操作日志数,
       o.final_version_id           AS 定稿版本ID,
       o.delivery_date              AS 交付日期
FROM orders o
LEFT JOIN users u             ON u.id = o.created_by
LEFT JOIN users d             ON d.id = o.designer_id
LEFT JOIN order_tasks t       ON t.order_id = o.id AND t.is_deleted = 0
LEFT JOIN design_versions v   ON v.task_id = t.id  AND v.is_deleted = 0
LEFT JOIN order_feedbacks f   ON f.order_id = o.id AND f.is_deleted = 0
LEFT JOIN order_reference_files r ON r.order_id = o.id AND r.is_deleted = 0
LEFT JOIN order_operation_logs l  ON l.order_id = o.id
WHERE o.is_deleted = 0
GROUP BY o.id, o.order_no, o.status, u.username, d.username, o.final_version_id, o.delivery_date
ORDER BY o.id;

-- ----------------------------------------------------------------------------
-- 4. 孤儿数据检查（全部期望 0 行）
-- ----------------------------------------------------------------------------
-- 4.1 任务归属不存在的订单
SELECT COUNT(*) AS orphan_task_order
FROM order_tasks t LEFT JOIN orders o ON o.id = t.order_id
WHERE o.id IS NULL;

-- 4.2 版本归属不存在的订单/任务
SELECT COUNT(*) AS orphan_version
FROM design_versions v
LEFT JOIN orders o      ON o.id = v.order_id
LEFT JOIN order_tasks t ON t.id = v.task_id
WHERE o.id IS NULL OR (v.task_id IS NOT NULL AND t.id IS NULL);

-- 4.3 意见归属不存在的订单/任务/版本
SELECT COUNT(*) AS orphan_feedback
FROM order_feedbacks f
LEFT JOIN orders o          ON o.id = f.order_id
LEFT JOIN order_tasks t     ON t.id = f.task_id
LEFT JOIN design_versions v ON v.id = f.version_id
WHERE o.id IS NULL
   OR (f.task_id IS NOT NULL AND t.id IS NULL)
   OR v.id IS NULL;

-- 4.4 参考附件归属不存在的订单/任务
SELECT COUNT(*) AS orphan_reference
FROM order_reference_files r
LEFT JOIN orders o      ON o.id = r.order_id
LEFT JOIN order_tasks t ON t.id = r.task_id
WHERE o.id IS NULL OR (r.task_id IS NOT NULL AND t.id IS NULL);

-- 4.5 日志归属不存在的订单
SELECT COUNT(*) AS orphan_log
FROM order_operation_logs l LEFT JOIN orders o ON o.id = l.order_id
WHERE o.id IS NULL;

-- ----------------------------------------------------------------------------
-- 5. 业务一致性抽查
-- ----------------------------------------------------------------------------
-- 5.1 已完结订单必须回填 final_version_id（期望 0 行）
SELECT id, order_no, status, final_version_id
FROM orders
WHERE status = 'FINISHED' AND final_version_id IS NULL;

-- 5.2 已完结订单的定稿版本必须属于本订单且 is_final = 1（期望 0 行）
SELECT o.id, o.order_no, o.final_version_id
FROM orders o
LEFT JOIN design_versions v ON v.id = o.final_version_id
WHERE o.status = 'FINISHED'
  AND (v.id IS NULL OR v.order_id <> o.id OR v.is_final <> 1);

-- 5.3 已分派状态必须齐备 designer_id / assigned_by / assigned_at（期望 0 行）
SELECT id, order_no, status, designer_id, assigned_by, assigned_at
FROM orders
WHERE status IN ('ASSIGNED', 'REVIEWING', 'FINISHED')
  AND (designer_id IS NULL OR assigned_by IS NULL OR assigned_at IS NULL);

-- 5.4 状态只允许 4 态（期望 0 行）
SELECT 'orders' AS 表, status AS 非法状态
FROM orders WHERE status NOT IN ('CREATED', 'ASSIGNED', 'REVIEWING', 'FINISHED')
UNION ALL
SELECT 'order_tasks', status FROM order_tasks
WHERE status NOT IN ('CREATED', 'ASSIGNED', 'REVIEWING', 'FINISHED');

-- 5.5 order_tasks.deadline 非空（期望 0 行）
SELECT id, task_no, deadline FROM order_tasks WHERE deadline IS NULL;

-- 5.6 同一订单内版本号唯一（期望 0 行）
SELECT order_id, version_no, COUNT(*) AS cnt
FROM design_versions
GROUP BY order_id, version_no
HAVING cnt > 1;

-- ----------------------------------------------------------------------------
-- 6. 数据量统计
-- ----------------------------------------------------------------------------
SELECT 'roles' AS 表名, COUNT(*) AS 行数 FROM roles
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_tasks', COUNT(*) FROM order_tasks
UNION ALL SELECT 'design_versions', COUNT(*) FROM design_versions
UNION ALL SELECT 'order_feedbacks', COUNT(*) FROM order_feedbacks
UNION ALL SELECT 'order_reference_files', COUNT(*) FROM order_reference_files
UNION ALL SELECT 'order_operation_logs', COUNT(*) FROM order_operation_logs;
