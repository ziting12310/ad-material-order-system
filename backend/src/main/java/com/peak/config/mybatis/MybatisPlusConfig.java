package com.peak.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 *
 * <p>注册两个核心插件：
 *  ① OptimisticLockerInnerInterceptor — 乐观锁（Entity 的 @Version 注解生效）
 *  ② PaginationInnerInterceptor — 分页插件（Controller 传 Page 对象时自动拦截）
 *
 * <p>对应 ARCHITECTURE.md §5.3 PER-005：乐观锁拦截并发冲突</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor mpInterceptor = new MybatisPlusInterceptor();

        // ① 乐观锁插件：Entity 的 @Version 字段自动处理
        // MP 在 updateById 时自动追加 WHERE version=? AND SET version=version+1
        mpInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // ② 分页插件：MySQL 方言
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);   // 单页最大 100 条
        mpInterceptor.addInnerInterceptor(pagination);

        return mpInterceptor;
    }
}
