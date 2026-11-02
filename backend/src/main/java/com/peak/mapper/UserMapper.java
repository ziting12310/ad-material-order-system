package com.peak.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peak.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper — schema.sql 第 2 张表 `users`
 * 继承 BaseMapper<User>，自带 insert / deleteById / updateById / selectById / selectList 等基础 CRUD。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 暂无自定义 SQL 方法 — MVP 阶段 CRUD 全由 BaseMapper 内置
}
