package com.peak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BCrypt 密码哈希工具测试 — 纯计算，无需 Spring 上下文
 *
 * <p>用途：
 *  1. 临时跑一下 {@link #encodePwd()}，拿到哈希串 → 生成 UPDATE SQL 回填数据库
 *  2. 跑 {@link #checkMatch()}，核对数据库里的哈希是否与明文 "123456" 匹配</p>
 */
@DisplayName("BCrypt 密码哈希")
public class PasswordHashTest {

    // 直接 new，不走 Spring 容器（BCryptPasswordEncoder 无状态，线程安全）
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密明文 "123456"，打印哈希值 + 可直接执行的 UPDATE SQL
     *
     * <p>同明文多次跑结果不同（BCrypt 自带随机盐），每次都是合法哈希。</p>
     */
    @Test
    @DisplayName("加密 123456 并输出 SQL")
    void encodePwd() {
        String raw = "123456";
        String hash = encoder.encode(raw);

        System.out.println("======= BCrypt Hash =======");
        System.out.println("明文    : " + raw);
        System.out.println("哈希串  : " + hash);
        System.out.println();
        System.out.println("—— 可直接执行的 SQL ——");
        System.out.println("UPDATE users SET password_hash='" + hash
                + "' WHERE username='am_demo';");
        System.out.println("===========================");
    }

    /**
     * 校验：明文 "123456" 是否匹配 BCrypt 哈希
     *
     * <p>两种用法：
     *  - 用本类 encodePwd 刚跑出来的哈希 → 必定通过
     *  - 把 dbHash 替换成数据库里实际存的哈希 → 验证种子数据是否正确</p>
     */
    @Test
    @DisplayName("校验 123456 与 BCrypt 哈希是否匹配")
    void checkMatch() {
        String raw = "123456";

        // 场景 A：本类现场生成的哈希（必定匹配）
        String freshHash = encoder.encode(raw);
        boolean freshOk = encoder.matches(raw, freshHash);
        System.out.println("现场哈希匹配: " + freshOk);
        assertTrue(freshOk, "BCrypt 自编码自校验应匹配");

        // 场景 B：把这里替换成数据库里 users.password_hash 的实际值
        //   String dbHash = "$2a$10$XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
        //   boolean dbOk = encoder.matches(raw, dbHash);
        //   System.out.println("数据库哈希匹配: " + dbOk);
    }
}
