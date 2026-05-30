package com.library.system.service.impl;

import com.library.system.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务实现（Redis模式）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!no-redis")
public class RedisTokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addToBlacklist(String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(token, "1", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Token已加入Redis黑名单: {}", token.substring(0, Math.min(20, token.length())) + "...");
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}
