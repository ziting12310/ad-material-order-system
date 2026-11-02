package com.peak.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peak.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单主表 Mapper — schema.sql 第 3 张表 `orders`（25 字段，含乐观锁 version）
 * 继承 BaseMapper<Order>，内置乐观锁 / 逻辑删除 / 分页（由 MybatisPlusConfig 插件开启）。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 暂无自定义 SQL 方法 — MVP 阶段 CRUD + 乐观锁全由 MP 内置
}
