package com.peak.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具类 — BCrypt 加密 / 校验
 *
 * <p>Spring Security Crypto 模块提供 BCryptPasswordEncoder，MVP 阶段启用 10 轮强度
 * （与 application.yml amoms.bcrypt-strength 一致，对应 ARCHITECTURE.md §5.1 SEC-006）。
 * BCrypt 自带随机盐 + 自适应算力，天然防彩虹表和暴力破解。</p>
 *
 * <p>与 BCryptConfig 的关系：
 *  - BCryptConfig 注册 BCryptPasswordEncoder Bean
 *  - 本类注入该 Bean，封装为业务语义的 encrypt / matches 方法
 *  - 直接使用 Bean 也能工作，本工具类的价值是语义统一 + 日志兜底</p>
 */
@Slf4j
@Component
public class PasswordUtil {

    /** Spring Security Crypto 提供的 BCrypt 编码器（Bean 由 BCryptConfig 注册） */
    private final BCryptPasswordEncoder encoder;

    public PasswordUtil(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    // ========================================================================
    // 【加密】encrypt — 用户注册 / 修改密码时调用
    // ========================================================================
    /**
     * BCrypt 加密明文密码
     *
     * @param rawPassword 明文（HTTPS 传输保障不被截获）
     * @return 60 位哈希串（形如 $2a$10$...），直接存 users.password_hash
     */
    public String encrypt(String rawPassword) {
        String hash = encoder.encode(rawPassword);
        log.debug("密码加密完成，哈希长度={}", hash.length());
        return hash;
    }

    // ========================================================================
    // 【比对】matches — 登录验证时调用
    // ========================================================================
    /**
     * BCrypt 校验明文密码与存储哈希是否匹配
     *
     * @param rawPassword   前端传来的明文
     * @param storedHash    DB 存储的哈希（users.password_hash）
     * @return true 匹配 / false 不匹配
     */
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            log.warn("密码比对参数为空: raw={}, hash={}", rawPassword != null, storedHash != null);
            return false;
        }
        return encoder.matches(rawPassword, storedHash);
    }
}
