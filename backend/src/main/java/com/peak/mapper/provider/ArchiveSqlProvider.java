package com.peak.mapper.provider;

import java.util.Map;

/**
 * 归档查询 SQL 构造器。
 *
 * <p>关键约束：订单表 LEFT JOIN 物料表（禁止 INNER JOIN），订单不会因为
 * 无物料/物料未定稿而被过滤掉；归档状态只放在 SELECT 的 CASE 聚合中计算。
 */
public class ArchiveSqlProvider {

    /** 归档状态聚合表达式：total > 0 且 finishNum = total → 已归档 */
    private static final String ARCHIVED_CASE =
            "CASE WHEN COUNT(t.id) > 0 "
                    + "AND COUNT(t.id) = SUM(CASE WHEN t.status IN ('APPROVED','DELIVERED') THEN 1 ELSE 0 END) "
                    + "THEN 1 ELSE 0 END";

    /** 归档状态文案：已归档 / 未归档 */
    private static final String ARCHIVE_STATUS_CASE =
            "CASE WHEN COUNT(t.id) > 0 "
                    + "AND COUNT(t.id) = SUM(CASE WHEN t.status IN ('APPROVED','DELIVERED') THEN 1 ELSE 0 END) "
                    + "THEN '已归档' ELSE '未归档' END";

    /** 归档时间：仅全部定稿时取最后一次定稿时间，否则为 NULL */
    private static final String ARCHIVED_AT_CASE =
            "CASE WHEN COUNT(t.id) > 0 "
                    + "AND COUNT(t.id) = SUM(CASE WHEN t.status IN ('APPROVED','DELIVERED') THEN 1 ELSE 0 END) "
                    + "THEN COALESCE(MAX(CASE WHEN t.status IN ('APPROVED','DELIVERED') THEN t.approved_at END), "
                    + "o.delivered_at, o.approved_at) "
                    + "ELSE NULL END";

    /** 分组查询列表（含已归档、未归档全部订单） */
    public String selectArchivePage(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT o.id, o.order_no, o.customer_name, o.material_type, o.quantity, ")
                .append("o.status, o.delivery_date, o.created_at, ")
                .append("COUNT(t.id) AS task_count, ")
                .append("COALESCE(SUM(CASE WHEN t.status IN ('APPROVED','DELIVERED') THEN 1 ELSE 0 END), 0) AS approved_task_count, ")
                .append(ARCHIVED_CASE).append(" AS archived, ")
                .append(ARCHIVE_STATUS_CASE).append(" AS archive_status, ")
                .append(ARCHIVED_AT_CASE).append(" AS archived_at ");
        appendFromWhere(sql, params);
        // 按订单内全部字段分组，兼容 ONLY_FULL_GROUP_BY
        sql.append(" GROUP BY o.id, o.order_no, o.customer_name, o.material_type, ")
                .append("o.quantity, o.status, o.delivery_date, o.created_at, ")
                .append("o.delivered_at, o.approved_at ")
                .append("ORDER BY o.id DESC LIMIT #{limit}");
        return sql.toString();
    }

    /** 已归档订单数量（同样基于 LEFT JOIN + GROUP BY + 聚合判定） */
    public String selectArchiveCount(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM (")
                .append("SELECT o.id ");
        appendFromWhere(sql, params);
        sql.append(" GROUP BY o.id HAVING COUNT(t.id) > 0 ")
                .append("AND COUNT(t.id) = SUM(CASE WHEN t.status IN ('APPROVED','DELIVERED') THEN 1 ELSE 0 END)")
                .append(") x");
        return sql.toString();
    }

    private void appendFromWhere(StringBuilder sql, Map<String, Object> params) {
        sql.append("FROM orders o ")
                .append("LEFT JOIN order_tasks t ON t.order_id = o.id ")
                .append("AND t.is_deleted = 0 AND t.status <> 'CANCELLED' ")
                .append("WHERE o.is_deleted = 0 ");

        String userRole = (String) params.get("userRole");
        if ("AM".equals(userRole)) {
            sql.append("AND o.created_by = #{userId} ");
        } else if ("DS".equals(userRole)) {
            sql.append("AND EXISTS (SELECT 1 FROM order_tasks dt WHERE dt.order_id = o.id ")
                    .append("AND dt.is_deleted = 0 AND dt.designer_id = #{userId}) ");
        }
        // BL：全量

        if (params.get("materialType") != null) {
            sql.append("AND o.material_type = #{materialType} ");
        }

        if (params.get("keyword") != null) {
            sql.append("AND (o.order_no LIKE CONCAT('%', #{keyword}, '%') ")
                    .append("OR o.customer_name LIKE CONCAT('%', #{keyword}, '%') ")
                    .append("OR EXISTS (SELECT 1 FROM order_tasks kt WHERE kt.order_id = o.id ")
                    .append("AND kt.is_deleted = 0 ")
                    .append("AND (kt.task_no LIKE CONCAT('%', #{keyword}, '%') ")
                    .append("OR kt.specification LIKE CONCAT('%', #{keyword}, '%')))) ");
        }

        if (params.get("deliveredDateFrom") != null) {
            sql.append("AND o.delivered_at >= #{deliveredDateFrom} ");
        }
        if (params.get("deliveredDateTo") != null) {
            sql.append("AND o.delivered_at <= #{deliveredDateTo} ");
        }
        // 注意：MyBatis 的 ParamMap 对不存在的 key 会抛 BindingException，
        // count 语句不传 lastId，必须先 containsKey 判断。
        Object lastId = params.containsKey("lastId") ? params.get("lastId") : null;
        if (lastId != null && ((Long) lastId) > 0) {
            sql.append("AND o.id < #{lastId} ");
        }
    }
}
