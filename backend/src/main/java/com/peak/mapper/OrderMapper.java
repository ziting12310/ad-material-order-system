package com.peak.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peak.entity.Order;
import com.peak.mapper.provider.ArchiveSqlProvider;
import com.peak.vo.OrderListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单主表 Mapper — schema.sql 第 3 张表 `orders`（25 字段，含乐观锁 version）
 * 继承 BaseMapper<Order>，内置乐观锁 / 逻辑删除 / 分页（由 MybatisPlusConfig 插件开启）。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 归档查询分页：订单 LEFT JOIN 物料，按订单分组统计物料定稿情况。
     * 返回全部订单（含已归档/未归档），归档状态在 SELECT 聚合中计算。
     */
    @SelectProvider(type = ArchiveSqlProvider.class, method = "selectArchivePage")
    List<OrderListItemVO> selectArchivePage(@Param("userId") Long userId,
                                            @Param("userRole") String userRole,
                                            @Param("materialType") String materialType,
                                            @Param("keyword") String keyword,
                                            @Param("deliveredDateFrom") LocalDate deliveredDateFrom,
                                            @Param("deliveredDateTo") LocalDate deliveredDateTo,
                                            @Param("taskDeadlineFrom") LocalDate taskDeadlineFrom,
                                            @Param("taskDeadlineTo") LocalDate taskDeadlineTo,
                                            @Param("lastId") Long lastId,
                                            @Param("limit") Integer limit);

    /** 已归档公司数量：与列表相同的 LEFT JOIN + 分组聚合口径，按公司名称去重计数 */
    @SelectProvider(type = ArchiveSqlProvider.class, method = "selectArchiveCount")
    long selectArchiveCount(@Param("userId") Long userId,
                            @Param("userRole") String userRole,
                            @Param("materialType") String materialType,
                            @Param("keyword") String keyword,
                            @Param("deliveredDateFrom") LocalDate deliveredDateFrom,
                            @Param("deliveredDateTo") LocalDate deliveredDateTo,
                            @Param("taskDeadlineFrom") LocalDate taskDeadlineFrom,
                            @Param("taskDeadlineTo") LocalDate taskDeadlineTo);
}
