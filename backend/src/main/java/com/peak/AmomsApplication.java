package com.peak;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

/**
 * AMOMS 启动类
 *
 * <p>Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.5 + JJWT 0.12.6</p>
 *
 * <p>关键注解：
 *  - @SpringBootApplication       → 自动配置 + 组件扫描（默认扫描 com.peak 及子包）
 *  - @MapperScan("com.peak.mapper") → MyBatis-Plus Mapper 接口扫描（避免每个 Mapper 加 @Mapper）
 *  - @OpenAPIDefinition          → Knife4j Swagger 3 文档元数据
 *
 * <p>⚠️ 临时禁用 Redis：exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class}
 *  本地无 Redis 时排除，生产环境部署 Redis 后移除 exclude 并恢复 @EnableCaching。</p>
 *
 * <p>启动成功后访问：
 *  - API 根路径：http://localhost:8080/api/v1/...
 *  - Knife4j 文档：http://localhost:8080/api/doc.html
 *  - Actuator 健康检查：http://localhost:8080/api/actuator/health</p>
 *
 * <p>启动命令：
 * <pre>mvn spring-boot:run -Dspring-boot.run.profiles=dev</pre></p>
 */
@Slf4j
@SpringBootApplication(
        // 临时排除 Redis 自动配置：本地无 Redis 环境时避免 ConnectException
        // 生产部署 Redis 后移除这两个 exclude 即可恢复 Redis 连接 + @EnableCaching
        exclude = {
                RedisAutoConfiguration.class,                // Spring Boot Redis 连接工厂 + RedisTemplate
                RedisRepositoriesAutoConfiguration.class     // Spring Data Redis Repository（如果用到）
        }
)
@MapperScan("com.peak.mapper")
@OpenAPIDefinition(
        info = @Info(
                title = "AMOMS — 广告制作公司宣传物料订单管理系统",
                version = "1.0.0",
                description = "MVP 版本：订单全生命周期管理（登记→分派→设计→修改→定稿→交付）\n"
                        + "技术栈：Spring Boot 3.2.5 / MyBatis-Plus 3.5.5 / JJWT 0.12.6 / MySQL 8.0"
        )
)
public class AmomsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmomsApplication.class, args);
    }

    /** 启动成功后打印 ASCII Banner，便于快速确认服务状态 */
    private static void printBanner() {
        log.info("\n"
                + "  ╔══════════════════════════════════════════════════════╗\n"
                + "  ║   AMOMS v1.0.0 — 广告制作公司宣传物料订单管理系统        ║\n"
                + "  ╠══════════════════════════════════════════════════════╣\n"
                + "  ║   API 根路径   : http://localhost:8080/api/v1        ║\n"
                + "  ║   Knife4j 文档 : http://localhost:8080/api/doc.html  ║\n"
                + "  ║   健康检查     : http://localhost:8080/api/actuator/health ║\n"
                + "  ║   Profile      : {}                                  ║\n"
                + "  ║   Java         : {}                                  ║\n"
                + "  ║   数据库       : MySQL 8.0 (amoms)                   ║\n"
                + "  ║   Redis        : ⚠️ 已临时禁用（exclude RedisAutoConfig）  ║\n"
                + "  ╚══════════════════════════════════════════════════════╝\n",
                System.getProperty("spring.profiles.active", "dev"),
                System.getProperty("java.version")
        );
    }
}
