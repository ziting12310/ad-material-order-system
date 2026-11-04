package com.peak.config.security;

import com.peak.common.constants.ErrorCode;
import com.peak.common.context.UserContext;
import com.peak.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 *
 * <p>职责：
 *  ① 从 request Header 取 Authorization（格式 "Bearer xxx"）
 *  ② 委托 JwtUtil 校验签名 + 过期 + 签发者
 *  ③ 提取 sub(userId) / role / name → 写入 UserContext + request.setAttribute（兼容旧代码）
 *  ④ 拒绝无令牌请求（401 UNAUTHORIZED）
 *  ⑤ afterCompletion 清除 UserContext（防止线程池复用脏读）
 *
 * <p>放行白名单：/v1/auth/login（登录接口本身不需要 Token）</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

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
        String token = authHeader.substring(prefix.length()).trim();

        // ③ 委托 JwtUtil 解析校验
        try {
            Claims claims = jwtUtil.parseToken(token);

            Long userId = JwtUtil.getUserId(claims);
            String role = JwtUtil.getRole(claims);
            String name = JwtUtil.getName(claims);

            // ④ 写入 UserContext（新方式）+ request 属性（兼容 B8 原有逻辑）
            UserContext.set(userId, role, name);
            request.setAttribute("userId", userId);
            request.setAttribute("userRole", role);
            request.setAttribute("userName", name);

            // TraceId：用 userId + 当前毫秒（兜底；生产用 SkyWalking 等注入）
            MDC.put("traceId", userId + "-" + System.currentTimeMillis());
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 过期: {}", e.getMessage());
            write401(response, ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
            return false;
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            write401(response, ErrorCode.TOKEN_INVALID, "无效的登录凭证");
            return false;
        }
    }

    /** 请求结束后清除 ThreadLocal，防止线程池复用导致用户态串号 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
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
