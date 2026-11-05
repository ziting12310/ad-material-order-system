package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.common.enums.UserRole;
import com.peak.config.security.TokenBlacklist;
import com.peak.dto.LoginRequest;
import com.peak.dto.RegisterRequest;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.UserMapper;
import com.peak.service.AuthService;
import com.peak.util.JwtUtil;
import com.peak.util.PasswordUtil;
import com.peak.vo.LoginResponseVO;
import com.peak.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务实现 — API.md §2.1 / §2.2
 *
 * <p>职责：密码校验 / JWT 签发 / 当前用户查询
 * JWT 签发统一委托 JwtUtil，密码校验统一委托 PasswordUtil，本类只做业务编排。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final TokenBlacklist tokenBlacklist;

    /** 登录失败阈值 */
    private static final int MAX_LOGIN_FAILURES = 5;
    /** 锁定时长：30 分钟 */
    private static final long LOCK_MILLIS = 30L * 60 * 1000;

    /** 用户名 → 失败次数 / 锁定截止时间（进程内实现） */
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    // ================================================================
    // 伪代码流程：
    // ① 按 username 查用户 → 不存在 → USER_NOT_FOUND
    // ② 校验 locked 字段 → 已锁定 → ACCOUNT_LOCKED
    // ③ BCrypt 校验密码（PasswordUtil）→ 不匹配 → LOGIN_FAILED
    // ④ 签发 JWT（JwtUtil：sub=userId, role, name；过期时间 2h）
    // ⑤ 更新 last_login_at
    // ⑥ 组装 LoginResponseVO 返回
    // ================================================================
    @Override
    public LoginResponseVO login(LoginRequest dto) {
        // ⓪ 连续失败锁定校验（5 次 / 30 分钟）
        LoginAttempt attempt = loginAttempts.get(dto.getUsername());
        if (attempt != null && attempt.lockUntil > System.currentTimeMillis()) {
            long remainMinutes = (attempt.lockUntil - System.currentTimeMillis()) / 60000 + 1;
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "账号已锁定，请 %d 分钟后再试", remainMinutes);
        }

        // ① 查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        // ② 锁定校验
        if (Integer.valueOf(1).equals(user.getLocked())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "账号已锁定，请联系管理员");
        }

        // ③ BCrypt 密码校验
        if (!passwordUtil.matches(dto.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(dto.getUsername());
            log.warn("登录失败：用户 {} 密码错误", dto.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }

        // 登录成功：清空失败计数
        loginAttempts.remove(dto.getUsername());

        // ④ 签发 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getName());

        // ⑤ 更新最后登录时间
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        // ⑥ 组装响应
        LoginResponseVO resp = new LoginResponseVO();
        resp.setToken(token);
        resp.setTokenType("Bearer");
        resp.setExpiresIn(jwtUtil.getExpireSeconds());
        resp.setUser(buildUserInfoVO(user));
        return resp;
    }

    @Override
    public UserInfoVO register(RegisterRequest dto) {
        String username = dto.getUsername().trim();

        // ① 账号唯一性校验（@TableLogic 自动过滤 is_deleted=1 的记录）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE, "账号已存在");
        }

        // ② 组装用户：密码 bcrypt 加密，locked/deleted/version 初始为 0
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordUtil.encrypt(dto.getPassword()));
        user.setName(dto.getName());
        // 自助注册固定为 DS，AM/BL 只能由内部维护，避免匿名注册获取业务权限
        user.setRole(UserRole.DS.getCode());
        user.setPhone(dto.getPhone());
        user.setLocked(0);
        user.setDeleted(0);
        // version 列由 DB 默认值 0 填充（User 实体无 @Version 字段）
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册 / 软删除记录占用 username 时，uk_username 兜底
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE, "账号已存在");
        }

        log.info("用户注册成功：id={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        // ③ 返回脱敏信息（不含密码哈希）
        return buildUserInfoVO(user);
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return buildUserInfoVO(user);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        try {
            long remainMillis = jwtUtil.parseToken(token).getExpiration().getTime()
                    - System.currentTimeMillis();
            tokenBlacklist.add(token, remainMillis);
        } catch (Exception e) {
            // Token 本身已失效，无需加入黑名单
            log.debug("登出时 Token 已失效：{}", e.getMessage());
        }
    }

    // ---------- 私有辅助方法 ----------

    /** 记录一次登录失败，达到阈值后锁定 30 分钟 */
    private void recordLoginFailure(String username) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(username, k -> new LoginAttempt());
        synchronized (attempt) {
            if (attempt.lockUntil > System.currentTimeMillis()) {
                return;
            }
            attempt.failures++;
            if (attempt.failures >= MAX_LOGIN_FAILURES) {
                attempt.lockUntil = System.currentTimeMillis() + LOCK_MILLIS;
                attempt.failures = 0;
                log.warn("账号 {} 连续登录失败 {} 次，锁定 30 分钟", username, MAX_LOGIN_FAILURES);
            }
        }
    }

    /** 登录失败计数载体 */
    private static final class LoginAttempt {
        private int failures;
        private long lockUntil;
    }

    /** Entity → VO（脱敏 phone） */
    private UserInfoVO buildUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        // phone 脱敏：138****1234（与 ARCH §5.1 SEC-004 一致）
        if (user.getPhone() != null && user.getPhone().length() == 11) {
            vo.setPhone(user.getPhone().substring(0, 3) + "****" + user.getPhone().substring(7));
        } else {
            vo.setPhone(user.getPhone());
        }
        return vo;
    }
}
