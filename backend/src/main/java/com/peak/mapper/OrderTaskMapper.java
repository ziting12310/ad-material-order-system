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

    /**
     * 行锁：对指定物料任务的 order_tasks 行加独占锁（FOR UPDATE），
     * 用于设计稿版本号原子生成：锁住父行后再查该任务版本号最大值 +1，
     * 避免并发上传时两个线程同时算到相同的 Vn+1。
     * <p>必须在已开启的事务里调用；返回该任务 id 表示行存在并已加锁。</p>
     */
    @Select("SELECT id FROM order_tasks WHERE id = #{taskId} FOR UPDATE")
    Long selectForUpdate(@Param("taskId") Long taskId);
}
