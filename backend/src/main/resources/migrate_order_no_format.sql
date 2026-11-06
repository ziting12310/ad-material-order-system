-- ============================================================================
-- 迁移：订单编号格式 ORD-YYYY-NNNNN → {公司名称}_NNN
-- 同公司从 001 开始递增
-- ============================================================================

-- 1. 先改列宽（VARCHAR(20) → VARCHAR(30)，防止长客户名溢出）
ALTER TABLE `orders` MODIFY COLUMN `order_no` VARCHAR(30) NOT NULL COMMENT '订单编号（{公司名称}_NNN，同公司从 001 开始递增）';

-- 2. 迁移数据：按 customer_name 分组，给每个公司的订单排序号
-- 使用用户变量实现窗口函数效果（MySQL 5.7+ 兼容写法）
SET @prev_customer := '', @seq := 0;

UPDATE orders o
JOIN (
    SELECT
        id,
        customer_name,
        @seq := IF(@prev_customer = customer_name, @seq + 1, 1) AS new_seq,
        @prev_customer := customer_name AS prev_tmp
    FROM (SELECT id, customer_name FROM orders WHERE is_deleted = 0 ORDER BY customer_name, id) sorted
) seq_map ON o.id = seq_map.id
SET o.order_no = CONCAT(seq_map.customer_name, '_', LPAD(seq_map.new_seq, 3, '0'));

-- 3. 验证
SELECT id, customer_name, order_no FROM orders ORDER BY customer_name, id;
