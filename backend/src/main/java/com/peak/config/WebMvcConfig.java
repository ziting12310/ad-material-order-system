package com.peak.config;

import com.peak.config.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：JWT 拦截器注册
 *
 * <p>职责：仅管拦截器注册；CORS 跨域已独立为 CorsConfig，避免职责重叠。
 * JWT 拦截器拦截 /v1/**，放行登录接口 + Knife4j 文档路径。</p>
 *
 * <p>放行白名单：
 *  - /v1/auth/login       — 登录接口本身不需要 Token
 *  - /doc.html 等         — Knife4j / Swagger 文档
 *  - OPTIONS 方法         — 由 JwtInterceptor 放行（CORS 预检）
 *
 * <p>注：/v1/files/presign 已移出白名单（原设计允许匿名申请上传凭证，存在被刷接口的风险），
 * 现统一要求 JWT，与 upload / confirm 保持一致。
 * 唯一的例外是 GET /v1/files/{fileId}（文件预览）：浏览器直接打开 URL 无法携带
 * Authorization，由 JwtInterceptor 在"无令牌"时单独放行；带令牌仍按数据范围校验。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /** 注册 JWT 拦截器 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/v1/**")                              // 拦截所有 API
                .excludePathPatterns(
                        "/v1/auth/login", "/v1/auth/login/",            // 登录放行（含尾斜杠变体）
                        "/v1/auth/register", "/v1/auth/register/",      // 自助注册放行（匿名可访问，含尾斜杠变体）
                        "/doc.html", "/webjars/**", "/v3/api-docs/**",
                        "/swagger-ui/**", "/swagger-resources/**"      // Knife4j / Swagger
                );
    }
}
