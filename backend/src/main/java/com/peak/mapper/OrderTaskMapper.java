package com.peak.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peak.entity.OrderTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderTaskMapper extends BaseMapper<OrderTask> {

    /**
     * 查询订单下最大排序号（包含已逻辑删除的任务）。
     * <p>确认物料清单会先逻辑删除旧任务再重建，若只看未删除记录，
     * 新任务号会与软删除记录撞 uk_order_task_no，因此这里用原生 SQL 绕过逻辑删除。</p>
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM order_tasks WHERE order_id = #{orderId}")
    Integer selectMaxSortOrderIncludingDeleted(@Param("orderId") Long orderId);
}
