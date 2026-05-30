package com.library.system.service.impl;

import com.library.system.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token黑名单服务实现（内存模式，仅用于测试）
 */
@Slf4j
@Service
@Profile("no-redis")
public class InMemoryTokenBlacklistServiceImpl implements TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void addToBlacklist(String token, long ttlSeconds) {
        long expireTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        blacklist.put(token, expireTime);
        log.debug("Token已加入内存黑名单(no-redis模式): {}", token.substring(0, Math.min(20, token.length())) + "...");
    }

    @Override
    public boolean isBlacklisted(String token) {
        Long expireTime = blacklist.get(token);
        if (expireTime == null) {
            return false;
        }
        if (System.currentTimeMillis() > expireTime) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
