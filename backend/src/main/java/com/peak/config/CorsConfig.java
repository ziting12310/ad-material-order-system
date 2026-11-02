package com.peak.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置
 *
 * <p>前后端分离架构：前端（Vue 3 dev 服务器端口 5173）→ 后端（端口 8080）。
 * 浏览器发送 AJAX 时自动带 Origin 头，本配置允许所有来源（MVP 阶段）。
 * 生产收紧 allowedOriginPatterns 到具体前端域名（如 https://app.amoms.com）。</p>
 *
 * <p>对应 ARCHITECTURE.md §6.4 AVA-003：
 * 允许所有 HTTP 方法（GET/POST/PUT/DELETE/OPTIONS），credentials=true 支持 Cookie/JWT 头跨域。</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // MVP 允许所有来源；生产改为具体域名
                .allowedOriginPatterns("*")
                // 允许所有 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // 允许所有请求头（含 Authorization）
                .allowedHeaders("*")
                // 允许携带凭证（Cookie / Authorization Header）
                .allowCredentials(true)
                // 预检请求缓存时间 1 小时（减少 OPTIONS 次数）
                .maxAge(3600);
    }
}
