package com.library.system.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存配置类
 * 作为一级缓存（L1），与Redis（L2）组成二级缓存架构
 *
 * FIXED: P2-001 实现L1→L2级联缓存，L1未命中时自动查询L2 Redis
 * FIXED: PERF-006 三个CacheManager各司其职，通过@Primary和Bean名称区分：
 *   - twoLevelCacheManager: 默认业务缓存（L1+L2）
 *   - caffeineLocalCacheManager: 纯本地缓存（不需要分布式共享时使用"caffeineLocalCacheManager"注入）
 *   - hotBooksCacheManager: 热门数据专用（更短的过期时间，使用"hotBooksCacheManager"注入）
 */
@Configuration
public class CaffeineConfig {

    @Value("${spring.cache.redis-ttl-minutes:30}")
    private long redisTtlMinutes;

    @Value("${spring.cache.prefix:'library:cache:'}")
    private String cachePrefix;

    /**
     * 二级缓存管理器（L1 Caffeine + L2 Redis）
     * 读取顺序: L1 → L2 → DB，回填L1+L2
     * 写入: 同时写入L1+L2
     * 删除: 同时失效L1+L2
     *
     * FIXED: P2-001 替换独立CaffeineCacheManager，实现真正级联缓存
     */
    @Bean
    @Primary
    @Profile("!no-redis")
    public CacheManager twoLevelCacheManager(RedisTemplate<String, Object> redisTemplate) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats()
                .softValues();

        String[] cacheNames = {"books", "readers", "hotBooks", "userSessions", "statistics", "announcements"};
        return new TwoLevelCacheManager(caffeine, redisTemplate, cacheNames,
                cachePrefix, redisTtlMinutes);
    }

    /**
     * 纯Caffeine缓存管理器（no-redis模式使用）
     * 注意：仅在 @Profile("no-redis") 激活时生效，不与 twoLevelCacheManager 冲突
     */
    @Bean
    @Profile("no-redis")
    public CacheManager noRedisCacheManager() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .softValues();

        String[] cacheNames = {"books", "readers", "hotBooks", "userSessions", "statistics", "announcements"};
        return new TwoLevelCacheManager(caffeine, null, cacheNames,
                "local:", redisTtlMinutes);
    }

    /**
     * 纯Caffeine缓存管理器（备用，不经过Redis）
     * 仅用于不需要分布式共享的本地缓存场景
     */
    @Bean("caffeineLocalCacheManager")
    public CacheManager caffeineLocalCacheManager() {
        com.github.benmanes.caffeine.cache.Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .softValues();

        String[] cacheNames = {"books", "readers", "hotBooks", "userSessions", "announcements"};
        return new TwoLevelCacheManager(caffeine, null, cacheNames,
                "local:", redisTtlMinutes);
    }

    /**
     * 配置热门图书缓存（高频访问数据）
     * 更短的过期时间，保证数据新鲜度
     */
    @Bean("hotBooksCacheManager")
    @Profile("!no-redis")
    public CacheManager hotBooksCacheManager(RedisTemplate<String, Object> redisTemplate) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();

        return new TwoLevelCacheManager(caffeine, redisTemplate,
                new String[]{"hotBooks"}, cachePrefix + "hot:", redisTtlMinutes);
    }
}
