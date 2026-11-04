package com.peak.common.annotation;

import com.peak.common.enums.UserRole;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解 — 标注在 Controller 方法上，由 RoleCheckAspect 拦截校验
 *
 * <p>用法示例：
 * <pre>
 * &#64;RequireRole({UserRole.BL})
 * public ResultVO&lt;...&gt; assignOrder(...) { ... }
 *
 * &#64;RequireRole({UserRole.AM, UserRole.BL})
 * public ResultVO&lt;...&gt; createOrder(...) { ... }
 * </pre>
 *
 * <p>校验逻辑：当前用户角色 ∈ value() 任一即放行，否则抛 PERMISSION_DENIED(403)。
 * 未登录（UserContext 为空）时抛 UNAUTHORIZED(401)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许访问的角色列表，任一命中即放行 */
    UserRole[] value();
}
