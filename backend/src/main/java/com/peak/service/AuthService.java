package com.peak.service;

import com.peak.dto.LoginRequest;
import com.peak.vo.LoginResponseVO;
import com.peak.vo.UserInfoVO;

/**
 * 认证服务接口 — API.md §2.1 / §2.2
 *
 * <p>职责：登录验证 / 密码校验 / JWT 签发 / Token 刷新 / 当前用户上下文</p>
 */
public interface AuthService {

    /**
     * 用户登录
     * <p>校验账号 + BCrypt 密码 + 锁定状态；成功后签发 JWT</p>
     *
     * @param dto 登录请求（username / password）
     * @return JWT Token + 用户信息
     * @throws com.peak.exception.BusinessException LOGIN_FAILED（密码错）/ ACCOUNT_LOCKED（锁定）/ USER_NOT_FOUND
     */
    LoginResponseVO login(LoginRequest dto);

    /**
     * 根据 userId 获取用户基础信息（缓存优先）
     * <p>用于 JWT 拦截器解析 Token 后，装载当前用户上下文</p>
     */
    UserInfoVO getUserInfo(Long userId);
}
