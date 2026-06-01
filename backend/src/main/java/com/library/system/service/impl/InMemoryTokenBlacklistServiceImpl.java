package com.library.system.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.library.system.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Profile("no-redis")
public class InMemoryTokenBlacklistServiceImpl implements TokenBlacklistService {

    private final Cache<String, Long> blacklist = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Long>() {
                @Override
                public long expireAfterCreate(String key, Long expireTimeNanos, long currentTime) {
                    return Math.max(expireTimeNanos - currentTime, 0);
                }

                @Override
                public long expireAfterUpdate(String key, Long expireTimeNanos, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(String key, Long expireTimeNanos, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .maximumSize(10_000)
            .build();

    @Override
    public void addToBlacklist(String token, long ttlSeconds) {
        long expireTimeNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(ttlSeconds);
        blacklist.put(token, expireTimeNanos);
        log.debug("Token已加入内存黑名单(no-redis模式): {}, TTL={}s", token.substring(0, Math.min(20, token.length())) + "...", ttlSeconds);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return blacklist.getIfPresent(token) != null;
    }
}
