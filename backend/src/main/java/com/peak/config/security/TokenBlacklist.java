package com.peak.config.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT Token 黑名单（进程内实现）
 *
 * <p>登出时把 Token 写入黑名单，TTL 与 Token 剩余有效期一致；
 * JwtInterceptor 在解析前先查黑名单，命中即返回 401。
 * 单机部署足够；集群部署可替换为 Redis 实现。</p>
 */
@Component
public class TokenBlacklist {

    /** token -> 过期时间戳（毫秒） */
    private final Map<String, Long> entries = new ConcurrentHashMap<>();

    /** 加入黑名单，ttlMillis <= 0 表示 Token 已过期，无需记录 */
    public void add(String token, long ttlMillis) {
        if (token == null || token.isBlank() || ttlMillis <= 0) return;
        entries.put(token, System.currentTimeMillis() + ttlMillis);
        if (entries.size() > 1024) {
            long now = System.currentTimeMillis();
            entries.entrySet().removeIf(e -> e.getValue() < now);
        }
    }

    /** 是否已登出（顺带清理过期条目） */
    public boolean contains(String token) {
        if (token == null) return false;
        Long expireAt = entries.get(token);
        if (expireAt == null) return false;
        if (expireAt < System.currentTimeMillis()) {
            entries.remove(token);
            return false;
        }
        return true;
    }
}
