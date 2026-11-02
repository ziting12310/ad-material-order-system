package com.peak.config.security;

import com.peak.common.constants.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * JWT 认证拦截器
 *
 * <p>职责：
 *  ① 从 request Header 取 Authorization（格式 "Bearer xxx"）
 *  ② 解析 JWT → 校验签名 + 过期
 *  ③ 提取 sub(userId) / role / name → request.setAttribute 注入（Controller 用 request.getAttribute("userId") 读取）
 *  ④ 拒绝无令牌请求（401 UNAUTHORIZED）
 *
 * <p>放行白名单：/v1/auth/login（登录接口本身不需要 Token）</p>
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${amoms.jwt.secret}")
    private String jwtSecret;

    @Value("${amoms.jwt.header}")
    private String headerName;

    @Value("${amoms.jwt.prefix}")
    private String prefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // ① 放行 OPTIONS（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ② 取 Authorization Header
        String authHeader = request.getHeader(headerName);
        if (authHeader == null || !authHeader.startsWith(prefix)) {
            write401(response, ErrorCode.UNAUTHORIZED, "请先登录");
            return false;
        }
        String token = authHeader.substring(prefix.length());

        // ③ 解析 JWT
        try {
            SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // ④ 注入 request + MDC
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);
            String name = claims.get("name", String.class);

            request.setAttribute("userId", userId);
            request.setAttribute("userRole", role);
            request.setAttribute("userName", name);
            // TraceId：用 userId + 当前毫秒（兜底；生产用 SkyWalking 等注入）
            MDC.put("traceId", userId + "-" + System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            if (e.getMessage().contains("expired")) {
                write401(response, ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
            } else {
                write401(response, ErrorCode.TOKEN_INVALID, "无效的登录凭证");
            }
            return false;
        }
    }

    /** 统一写 401 响应 */
    private void write401(HttpServletResponse response, String code, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"data\":null,\"traceId\":\""
                            + MDC.get("traceId") + "\"}"
            );
        } catch (Exception ignored) {}
    }
}
