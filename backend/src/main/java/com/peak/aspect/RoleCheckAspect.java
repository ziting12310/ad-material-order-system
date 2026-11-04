package com.peak.aspect;

import com.peak.common.annotation.RequireRole;
import com.peak.common.constants.ErrorCode;
import com.peak.common.context.UserContext;
import com.peak.common.enums.UserRole;
import com.peak.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 角色权限切面 — 拦截所有标注 @RequireRole 的方法
 *
 * <p>校验顺序：
 *  ① UserContext 为空（未登录/Token 无效被拦截器放行到此属于异常） → 401 UNAUTHORIZED
 *  ② 当前角色不在注解允许列表内 → 403 PERMISSION_DENIED
 *  ③ 通过 → 放行执行业务方法
 *
 * <p>与 JwtInterceptor 的关系：拦截器负责"是否登录"（401），本切面负责"是否有权"（403）。
 */
@Slf4j
@Aspect
@Component
public class RoleCheckAspect {

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        // ① 登录态校验
        if (!UserContext.isLogin()) {
            log.warn("权限切面拦截：未登录访问 {}.{}",
                    pjp.getSignature().getDeclaringTypeName(), pjp.getSignature().getName());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        // ② 角色校验
        String currentRole = UserContext.getRole();
        boolean allowed = Arrays.stream(requireRole.value())
                .anyMatch(r -> r.name().equals(currentRole));

        if (!allowed) {
            String allowedRoles = Arrays.stream(requireRole.value())
                    .map(UserRole::name)
                    .collect(Collectors.joining("/"));
            log.warn("权限切面拦截：用户[id={}, role={}] 无权访问 {}.{}（要求角色：{}）",
                    UserContext.getUserId(), currentRole,
                    pjp.getSignature().getDeclaringTypeName(), pjp.getSignature().getName(),
                    allowedRoles);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "无权限执行此操作，要求角色：" + allowedRoles);
        }

        // ③ 放行
        return pjp.proceed();
    }
}
