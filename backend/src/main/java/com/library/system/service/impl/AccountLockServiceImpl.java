package com.library.system.service.impl;

import com.library.system.entity.User;
import com.library.system.mapper.UserMapper;
import com.library.system.service.AccountLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 账户锁定服务实现类
 * FIXED: SEC-002 使用Redis进行分布式账户锁定（替代Caffeine本地缓存）
 * 
 * 使用Redis存储：
 * 1. 登录失败计数（带TTL自动过期）
 * 2. 账户锁定状态（带TTL自动解锁）
 * 
 * @author Security Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!no-redis")
public class AccountLockServiceImpl implements AccountLockService {

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    private static final String FAIL_COUNT_PREFIX = "login:fail:count:";
    private static final String LOCK_KEY_PREFIX = "account:locked:";

    @Override
    public boolean isLocked(Long userId) {
        String lockKey = LOCK_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    @Override
    public boolean isLockedByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return false;
        }
        return isLocked(user.getId());
    }

    @Override
    public int recordLoginFailure(Long userId, String username) {
        String countKey = FAIL_COUNT_PREFIX + userId;
        
        // 增加失败计数
        Long count = redisTemplate.opsForValue().increment(countKey);
        
        // 设置15分钟过期
        redisTemplate.expire(countKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        
        int failCount = count != null ? count.intValue() : 1;
        
        log.debug("记录登录失败: userId={}, username={}, failCount={}", userId, username, failCount);
        
        // 连续失败5次，锁定账户
        if (failCount >= MAX_LOGIN_FAILURES) {
            lockAccount(userId, username);
        }
        
        return failCount;
    }

    @Override
    public void clearLoginFailures(Long userId) {
        String countKey = FAIL_COUNT_PREFIX + userId;
        redisTemplate.delete(countKey);
        log.debug("清除登录失败计数: userId={}", userId);
    }

    @Override
    public long getRemainingLockSeconds(Long userId) {
        String lockKey = LOCK_KEY_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    @Override
    public void unlockAccount(Long userId) {
        String lockKey = LOCK_KEY_PREFIX + userId;
        String countKey = FAIL_COUNT_PREFIX + userId;
        
        redisTemplate.delete(lockKey);
        redisTemplate.delete(countKey);
        
        log.info("账户已解锁: userId={}", userId);
    }

    @Override
    public int getLoginFailCount(Long userId) {
        String countKey = FAIL_COUNT_PREFIX + userId;
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 锁定账户
     */
    private void lockAccount(Long userId, String username) {
        String lockKey = LOCK_KEY_PREFIX + userId;
        
        // 设置锁定状态，15分钟后自动解锁
        redisTemplate.opsForValue().set(lockKey, 
                String.format("locked_at:%d", System.currentTimeMillis()), 
                LOCK_DURATION_MINUTES, 
                TimeUnit.MINUTES);
        
        log.warn("账户已被锁定: userId={}, username={}, 锁定时长: {}分钟", 
                userId, username, LOCK_DURATION_MINUTES);
    }
}
