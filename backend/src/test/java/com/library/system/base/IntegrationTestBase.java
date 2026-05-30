package com.library.system.base;

import com.library.system.LibraryApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试基类 - 使用 Testcontainers 启动 MySQL 和 Redis
 * 所有集成测试应继承此类
 */
@SpringBootTest(
    classes = LibraryApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // MySQL Testcontainer
    @Container
    protected static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0")
    )
            .withDatabaseName("library_system_test")
            .withUsername("root")
            .withPassword("test")
            .withReuse(true);

    // Redis Testcontainer
    @Container
    protected static final GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")
    )
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 动态注入 Redis 连接信息
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("redisson.address", () ->
                "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379));
        // 使用 Testcontainers JDBC URL
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

    @BeforeAll
    static void setupContainers() {
        // 容器启动后初始化
    }

    @AfterAll
    static void cleanup() {
        // 测试结束后清理资源
    }

    @BeforeEach
    void setUpIntegration() {
        log.info("=== 集成测试开始: {} ===", getClass().getSimpleName());
    }

    /**
     * 断言异常消息包含指定文本
     */
    protected void assertExceptionMessage(
            Class<? extends Throwable> expectedType,
            org.junit.jupiter.api.function.Executable executable,
            String expectedMessageContains
    ) {
        Throwable thrown = assertThrows(expectedType, executable);
        if (expectedMessageContains != null && !expectedMessageContains.isEmpty()) {
            assertTrue(thrown.getMessage().contains(expectedMessageContains),
                    "期望异常消息包含: \"" + expectedMessageContains + "\", 实际: \"" + thrown.getMessage() + "\"");
        }
    }
}
