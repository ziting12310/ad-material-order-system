package com.peak.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码编码器配置
 *
 * <p>提供 BCryptPasswordEncoder Bean，AuthServiceImpl 注入使用。
 * 密码加密强度 10 轮（与 ARCHITECTURE.md §5.1 SEC-006 一致；可通过 application.yml amoms.bcrypt-strength 调整）。</p>
 */
@Configuration
public class BCryptConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        // 10 轮：2^10 = 1024 次哈希运算，平衡安全性与性能
        return new BCryptPasswordEncoder(10);
    }
}
