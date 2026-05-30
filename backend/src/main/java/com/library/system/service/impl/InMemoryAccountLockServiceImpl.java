package com.library.system.service.impl;

import com.library.system.service.AccountLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 账户锁定服务实现类（no-redis模式）
 * 使用内存存储替代Redis，仅用于测试环境
 *
 * @author Security Team
 * @version 2.0.0
 */
@Slf4j
@Service
@Profile("no-redis")
public class InMemoryAccountLockServiceImpl implements AccountLockService {

    private final Map<Long, Integer> failCountMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> lockEndTimeMap = new ConcurrentHashMap<>();

    @Override
    public boolean isLocked(Long userId) {
        Long endTime = lockEndTimeMap.get(userId);
        if (endTime == null) {
            return false;
        }
        if (System.currentTimeMillis() > endTime) {
            lockEndTimeMap.remove(userId);
            return false;
        }
        return true;
    }

    @Override
    public boolean isLockedByUsername(String username) {
        return false;
    }

    @Override
    public int recordLoginFailure(Long userId, String username) {
        Integer currentCount = failCountMap.compute(userId, (k, v) -> v == null ? 1 : v + 1);
        int failCount = currentCount != null ? currentCount : 1;

        log.debug("记录登录失败(内存模式): userId={}, username={}, failCount={}", userId, username, failCount);

        if (failCount >= MAX_LOGIN_FAILURES) {
            lockAccount(userId, username);
        }

        return failCount;
    }

    @Override
    public void clearLoginFailures(Long userId) {
        failCountMap.remove(userId);
        log.debug("清除登录失败计数(内存模式): userId={}", userId);
    }

    @Override
    public long getRemainingLockSeconds(Long userId) {
        Long endTime = lockEndTimeMap.get(userId);
        if (endTime == null) {
            return 0;
        }
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    @Override
    public void unlockAccount(Long userId) {
        lockEndTimeMap.remove(userId);
        failCountMap.remove(userId);
        log.info("账户已解锁(内存模式): userId={}", userId);
    }

    @Override
    public int getLoginFailCount(Long userId) {
        return failCountMap.getOrDefault(userId, 0);
    }

    private void lockAccount(Long userId, String username) {
        long endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(LOCK_DURATION_MINUTES);
        lockEndTimeMap.put(userId, endTime);

        log.warn("账户已被锁定(内存模式): userId={}, username={}, 锁定时长: {}分钟",
                userId, username, LOCK_DURATION_MINUTES);
    }
}
