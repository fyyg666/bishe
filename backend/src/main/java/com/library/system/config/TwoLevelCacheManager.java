package com.library.system.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 二级缓存管理器
 * L1(Caffeine本地缓存) → L2(Redis分布式缓存) → 数据库
 *
 * 缓存读写流程：
 * - 读: L1 → L2 → DB，回填L1+L2
 * - 写: 同时失效L1+L2
 *
 * FIXED: P2-001 实现真正的级联缓存，L1未命中时查询L2
 */
@Slf4j
public class TwoLevelCacheManager implements CacheManager {

    private final ConcurrentHashMap<String, TwoLevelCache> caches = new ConcurrentHashMap<>();
    private final Caffeine<Object, Object> caffeine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String cachePrefix;
    private final long redisTtlMinutes;

    /**
     * @param caffeine        Caffeine配置
     * @param redisTemplate   Redis模板
     * @param cacheNames      缓存名称列表
     * @param cachePrefix     Redis key前缀
     * @param redisTtlMinutes Redis缓存过期时间（分钟）
     */
    public TwoLevelCacheManager(Caffeine<Object, Object> caffeine,
                                RedisTemplate<String, Object> redisTemplate,
                                String[] cacheNames,
                                String cachePrefix,
                                long redisTtlMinutes) {
        this.caffeine = caffeine;
        this.redisTemplate = redisTemplate;
        this.cachePrefix = cachePrefix;
        this.redisTtlMinutes = redisTtlMinutes;
        for (String name : cacheNames) {
            caches.put(name, new TwoLevelCache(name));
        }
    }

    @Override
    public org.springframework.cache.Cache getCache(String name) {
        return caches.computeIfAbsent(name, TwoLevelCache::new);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }

    /**
     * 二级缓存实现
     * L1: Caffeine本地缓存（毫秒级响应，最大5000条，30分钟过期）
     * L2: Redis分布式缓存（集群共享，1小时过期）
     */
    class TwoLevelCache extends AbstractValueAdaptingCache {

        private final String name;
        private final Cache<Object, Object> l1Cache;
        private final String redisKeyPrefix;

        protected TwoLevelCache(String name) {
            super(true); // allowNullValues = true — 允许缓存null值以缓存空结果防止重复查询
            this.name = name;
            this.l1Cache = caffeine.build();
            this.redisKeyPrefix = cachePrefix + name + ":";
        }

        @Override
        protected Object lookup(Object key) {
            // L1查找
            Object value = l1Cache.getIfPresent(key);
            if (value != null) {
                log.debug("L1缓存命中: cache={}, key={}", name, key);
                return value;
            }

            // L2查找（Redis），如redisTemplate为null则直接降级
            if (redisTemplate != null) {
                try {
                    String redisKey = redisKeyPrefix + key;
                    value = redisTemplate.opsForValue().get(redisKey);
                    if (value != null) {
                        log.debug("L2缓存命中,回填L1: cache={}, key={}", name, key);
                        l1Cache.put(key, value);
                    }
                } catch (Exception e) {
                    log.warn("L2缓存查询异常,降级到数据库: cache={}, key={}, error={}", name, key, e.getMessage());
                }
            }

            return value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return l1Cache;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            Object value = lookup(key);
            if (value != null) {
                return (T) value;
            }

            // L1和L2都未命中，从数据源加载
            try {
                T loadedValue = valueLoader.call();
                if (loadedValue != null) {
                    put(key, loadedValue);
                }
                return loadedValue;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            if (value == null) {
                evict(key);
                return;
            }
            // 写入L1
            l1Cache.put(key, value);
            // 写入L2（Redis），如redisTemplate为null则跳过
            if (redisTemplate != null) {
                try {
                    String redisKey = redisKeyPrefix + key;
                    redisTemplate.opsForValue().set(redisKey, value,
                            java.time.Duration.ofMinutes(redisTtlMinutes));
                } catch (Exception e) {
                    log.warn("L2缓存写入异常: cache={}, key={}, error={}", name, key, e.getMessage());
                }
            }
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            Object existing = lookup(key);
            if (existing != null) {
                return () -> existing;
            }
            put(key, value);
            return null;
        }

        @Override
        public void evict(Object key) {
            // 清除L1
            l1Cache.invalidate(key);
            // 清除L2（Redis），如redisTemplate为null则跳过
            if (redisTemplate != null) {
                try {
                    String redisKey = redisKeyPrefix + key;
                    redisTemplate.delete(redisKey);
                } catch (Exception e) {
                    log.warn("L2缓存删除异常: cache={}, key={}, error={}", name, key, e.getMessage());
                }
            }
        }

        @Override
        public boolean evictIfPresent(Object key) {
            boolean l1Evicted = l1Cache.asMap().remove(key) != null;
            if (redisTemplate != null) {
                try {
                    String redisKey = redisKeyPrefix + key;
                    Boolean deleted = redisTemplate.delete(redisKey);
                    return l1Evicted || Boolean.TRUE.equals(deleted);
                } catch (Exception e) {
                    log.warn("L2缓存删除异常: cache={}, key={}", name, key);
                    return l1Evicted;
                }
            }
            return l1Evicted;
        }

        @Override
        public void clear() {
            l1Cache.invalidateAll();
            if (redisTemplate != null) {
                try {
                    Set<String> keys = new HashSet<>();
                    ScanOptions options = ScanOptions.scanOptions()
                            .match(redisKeyPrefix + "*")
                            .count(100)
                            .build();
                    try (Cursor<String> cursor = redisTemplate.scan(options)) {
                        while (cursor.hasNext()) {
                            keys.add(cursor.next());
                        }
                    }
                    if (!keys.isEmpty()) {
                        redisTemplate.delete(keys);
                    }
                } catch (Exception e) {
                    log.warn("L2缓存清空异常: cache={}, error={}", name, e.getMessage());
                }
            }
        }

        @Override
        public boolean invalidate() {
            clear();
            return true;
        }
    }
}
