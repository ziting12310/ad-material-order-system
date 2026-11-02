package com.peak.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peak.common.constants.ErrorCode;
import com.peak.dto.LoginRequest;
import com.peak.entity.User;
import com.peak.exception.BusinessException;
import com.peak.mapper.UserMapper;
import com.peak.service.AuthService;
import com.peak.vo.LoginResponseVO;
import com.peak.vo.UserInfoVO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * 认证服务实现 — API.md §2.1 / §2.2
 *
 * <p>职责：密码校验 / JWT 签发 / 当前用户查询</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;   // 由 config/security 配置类注册 Bean

    @Value("${amoms.jwt.secret}")
    private String jwtSecret;

    @Value("${amoms.jwt.expire-seconds}")
    private long expireSeconds;

    // ================================================================
    // 伪代码流程：
    // ① 按 username 查用户 → 不存在 → USER_NOT_FOUND
    // ② 校验 locked 字段 → 已锁定 → ACCOUNT_LOCKED
    // ③ BCrypt 校验密码 → 不匹配 → LOGIN_FAILED
    // ④ 签发 JWT（payload: sub=userId, role, name；过期时间 2h）
    // ⑤ 更新 last_login_at
    // ⑥ 组装 LoginResponseVO 返回
    // ================================================================
    @Override
    public LoginResponseVO login(LoginRequest dto) {
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
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.warn("登录失败：用户 {} 密码错误", dto.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }

        // ④ 签发 JWT
        String token = generateToken(user);

        // ⑤ 更新最后登录时间
        user.setLastLoginAt(new Date(System.currentTimeMillis())
                .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        userMapper.updateById(user);

        // ⑥ 组装响应
        LoginResponseVO resp = new LoginResponseVO();
        resp.setToken(token);
        resp.setTokenType("Bearer");
        resp.setExpiresIn(expireSeconds);
        resp.setUser(buildUserInfoVO(user));
        return resp;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return buildUserInfoVO(user);
    }

    // ---------- 私有辅助方法 ----------

    /** 签发 JWT — jjwt 0.12.x */
    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))          // sub = userId
                .claim("role", user.getRole())                  // role
                .claim("name", user.getName())                  // 姓名（方便拦截器直接读）
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .issuer("AMOMS")
                .signWith(key)
                .compact();
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
