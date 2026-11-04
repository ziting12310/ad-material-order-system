package com.peak.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 — 统一 JWT 签发 / 解析 / 校验
 *
 * <p>jjwt 0.12.x 强制使用 256-bit+ 密钥（Base64 编码后至少 32 字节）。
 * 本工具类是无状态的纯计算 Bean，不依赖数据库；Redis Token 黑名单可在拦截器层叠加。</p>
 *
 * <p>Payload 结构（统一规范）：
 * <pre>
 * {
 *   "sub": "1",           // userId（字符串）
 *   "role": "AM",         // 角色编码
 *   "name": "张三",       // 姓名
 *   "iat": 1730000000,    // 签发时间
 *   "exp": 1730007200,    // 过期时间
 *   "iss": "AMOMS"        // 签发者
 * }
 * </pre></p>
 *
 * <p>对应 ARCHITECTURE.md §6.3 缓存策略：auth:token:{jti} TTL 2h（与 expire-seconds 对齐）</p>
 */
@Slf4j
@Component
public class JwtUtil {

    /** 密钥（Base64 编码 256-bit，配置在 application.yml amoms.jwt.secret） */
    private final SecretKey secretKey;

    /** 过期秒数 */
    private final long expireSeconds;

    /** 签发者 */
    private static final String ISSUER = "AMOMS";

    // ---------- 构造注入 ----------
    public JwtUtil(@Value("${amoms.jwt.secret}") String jwtSecret,
                   @Value("${amoms.jwt.expire-seconds}") long expireSeconds) {
        // Base64 解码 → SecretKey（jjwt 0.12.x 强制 HMAC-SHA256 密钥 ≥ 256-bit）
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        this.expireSeconds = expireSeconds;
    }

    /** 过期秒数（供登录响应组装 expiresIn 字段） */
    public long getExpireSeconds() {
        return expireSeconds;
    }

    // ========================================================================
    // 【签发】generateToken — 用户登录时调用
    // ========================================================================
    /**
     * 签发 JWT
     *
     * @param userId 用户 ID（写入 sub claim）
     * @param role   用户角色（AM / BL / DS）
     * @param name   用户姓名（方便拦截器直接读，避免再查库）
     * @return JWT 字符串（Bearer 前缀由拦截器约定，本方法不带）
     */
    public String generateToken(Long userId, String role, String name) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("name", name);
        return Jwts.builder()
                .subject(String.valueOf(userId))     // sub = userId
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .issuer(ISSUER)
                .signWith(secretKey)
                .compact();
    }

    // ========================================================================
    // 【解析】parseToken — 拦截器调用
    // ========================================================================
    /**
     * 解析 JWT Claims（不校验过期）
     *
     * @throws SignatureException     签名错误
     * @throws ExpiredJwtException    Token 过期
     * @throws IllegalArgumentException 格式错误
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ========================================================================
    // 【校验】validateToken — 拦截器 preHandle 调用
    // ========================================================================
    /**
     * 校验 Token 有效性（签名 + 过期 + 签发者）
     *
     * @return true 有效 / false 无效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 签发者校验（防止伪造）
            if (!ISSUER.equals(claims.getIssuer())) {
                log.warn("JWT 签发者不匹配: {}", claims.getIssuer());
                return false;
            }
            // 过期时间已由 parseToken 抛出 ExpiredJwtException
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // 【便捷提取】静态方法 — 拦截器/Service 层快速读 claim
    // ========================================================================
    /** 从 Token Claims 中取 userId（sub） */
    public static Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /** 从 Token Claims 中取 role */
    public static String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    /** 从 Token Claims 中取 name */
    public static String getName(Claims claims) {
        return claims.get("name", String.class);
    }
}
