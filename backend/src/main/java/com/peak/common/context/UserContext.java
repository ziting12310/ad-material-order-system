package com.peak.common.context;

/**
 * 当前登录用户上下文 — 基于 ThreadLocal 的请求级用户态
 *
 * <p>写入时机：JwtInterceptor.preHandle 解析 Token 成功后写入；
 * 清除时机：JwtInterceptor.afterCompletion 请求结束后清除（防止线程池复用导致脏读）。
 *
 * <p>读取方式：Service / Controller 任意层可直接 {@code UserContext.getUserId()}，
 * 无需从 HttpServletRequest 透传，降低层间耦合。
 */
public final class UserContext {

    private UserContext() {}

    /** 当前请求的用户信息载体 */
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    /** 写入当前用户（仅拦截器调用） */
    public static void set(Long userId, String role, String name) {
        HOLDER.set(new CurrentUser(userId, role, name));
    }

    /** 当前用户 ID */
    public static Long getUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    /** 当前用户角色编码（AM / BL / DS） */
    public static String getRole() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.role();
    }

    /** 当前用户姓名 */
    public static String getName() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.name();
    }

    /** 判断是否已登录（拦截器已写入上下文） */
    public static boolean isLogin() {
        return HOLDER.get() != null;
    }

    /** 清除上下文（仅拦截器在请求结束时调用） */
    public static void clear() {
        HOLDER.remove();
    }

    /** 不可变载体 */
    private record CurrentUser(Long userId, String role, String name) {}
}
