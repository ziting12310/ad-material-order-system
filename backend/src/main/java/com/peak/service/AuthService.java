package com.peak.service;

import com.peak.dto.ChangePasswordRequest;
import com.peak.dto.LoginRequest;
import com.peak.dto.RegisterRequest;
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
     * 自助注册
     * <p>校验账号唯一性，密码 bcrypt 加密后落库，新增用户固定为 DS 角色，返回脱敏用户信息（不含密码）</p>
     *
     * @param dto 注册请求（username / password / name / phone）
     * @return 注册成功的用户基本信息
     * @throws com.peak.exception.BusinessException USERNAME_DUPLICATE（账号已存在）
     */
    UserInfoVO register(RegisterRequest dto);

    /**
     * 根据 userId 获取用户基础信息（缓存优先）
     * <p>用于 JWT 拦截器解析 Token 后，装载当前用户上下文</p>
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 退出登录：把当前 Token 加入黑名单，直到其自然过期
     *
     * @param token 不带 Bearer 前缀的 JWT；为空或已失效时静默忽略
     */
    void logout(String token);

    /**
     * 自助修改密码（需登录）
     * <p>先 BCrypt 校验原密码，通过后加密新密码更新到 DB。</p>
     *
     * @param userId 当前登录用户 ID
     * @param dto 修改密码请求（oldPassword / newPassword）
     * @throws com.peak.exception.BusinessException 原密码错误 / 用户不存在
     */
    void changePassword(Long userId, ChangePasswordRequest dto);
}
