package com.library.system.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Set;

/**
 * Redis配置类
 * 配置RedisTemplate、CacheManager和RedissonClient
 *
 * FIXED: P2-002 使用BasicPolymorphicTypeValidator白名单替代LaissezFaireSubTypeValidator，
 * 防止反序列化任意类的安全风险
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${redisson.address:redis://localhost:6379}")
    private String redissonAddress;

    @Value("${redisson.password:}")
    private String redissonPassword;

    /**
     * 安全反序列化白名单 - 允许的Java基础类型包前缀
     * FIXED: SEC-P2-01 定义集中管理的反序列化白名单常量
     */
    private static final Set<String> ALLOWED_DESERIALIZE_PACKAGES = Set.of(
            "com.library.system.",
            "java.util.",
            "java.lang.",
            "java.time.",
            "java.math.",
            "[Ljava.lang.",
            "[Lcom.library.system."
    );

    /**
     * 创建安全的ObjectMapper，使用类型白名单
     * FIXED: SEC-P2-01 替换LaissezFaireSubTypeValidator为安全白名单，
     * 防止反序列化任意类的安全风险
     */
    private ObjectMapper createSafeObjectMapper() {
        // 白名单：仅允许反序列化库系统内的DTO和Java基础类型
        BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class);
        for (String prefix : ALLOWED_DESERIALIZE_PACKAGES) {
            builder.allowIfSubType(prefix);
        }
        PolymorphicTypeValidator ptv = builder.build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

    /**
     * 配置RedisTemplate
     * FIXED: P2-002 使用安全白名单序列化器
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(createSafeObjectMapper());
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置RedisCacheManager（独立Redis缓存管理器，备用）
     * 注意：主缓存管理器已迁移到CaffeineConfig.twoLevelCacheManager (L1+L2)
     *
     * FIXED: P2-002 使用安全白名单序列化器
     */
    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(createSafeObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        RedisCacheConfiguration bookConfig = defaultConfig.entryTtl(Duration.ofMinutes(30));
        RedisCacheConfiguration userConfig = defaultConfig.entryTtl(Duration.ofHours(2));
        RedisCacheConfiguration hotBooksConfig = defaultConfig.entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("books", bookConfig)
                .withCacheConfiguration("users", userConfig)
                .withCacheConfiguration("hotBooks", hotBooksConfig)
                .transactionAware()
                .build();
    }

    /**
     * 配置RedissonClient（用于分布式锁）
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(redissonAddress)
                .setPassword(redissonPassword.isEmpty() ? null : redissonPassword)
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(32)  // FIXED: PERF-005 从64调整为32，单节点推荐值
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
