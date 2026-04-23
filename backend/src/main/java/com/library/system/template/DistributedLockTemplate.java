package com.library.system.template;

import com.library.system.common.Constants;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁模板类
 * <p>
 * 封装分布式锁的获取、执行业务逻辑、释放锁的通用模式，
 * 消除代码中的重复锁处理逻辑。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 有返回值
 * SeatReservation result = lockTemplate.executeWithLock(
 *     "lock:key:" + id,
 *     () -> {
 *         // 业务逻辑
 *         return doSomething();
 *     }
 * );
 *
 * // 无返回值
 * lockTemplate.executeWithLock(
 *     "lock:key:" + id,
 *     () -> {
 *         // 业务逻辑
 *         doSomething();
 *     }
 * );
 * }</pre>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2026-04-24
 */
@Slf4j
@RequiredArgsConstructor
public class DistributedLockTemplate {

    private final RedissonClient redissonClient;

    /**
     * 默认等待时间（秒）
     */
    private static final int DEFAULT_WAIT_TIME = 5;

    /**
     * 默认锁持有时间（秒）
     */
    private static final int DEFAULT_LEASE_TIME = 30;

    /**
     * 使用分布式锁执行有返回值的业务逻辑
     *
     * @param lockKey 锁的键
     * @param action  业务逻辑（Supplier，有返回值）
     * @return 业务逻辑的返回值
     * @param <T> 返回值类型
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = tryAcquireLock(lock, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME);
            if (!locked) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        Constants.LockMessage.ACQUIRE_FAILED);
            }

            log.debug("分布式锁获取成功: lockKey={}", lockKey);
            return action.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("分布式锁操作被中断: lockKey={}", lockKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    Constants.LockMessage.OPERATION_INTERRUPTED);

        } finally {
            releaseLock(lock, locked, lockKey);
        }
    }

    /**
     * 使用分布式锁执行无返回值的业务逻辑
     *
     * @param lockKey 锁的键
     * @param action  业务逻辑（Runnable，无返回值）
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 使用分布式锁执行有返回值的业务逻辑（自定义超时时间）
     *
     * @param lockKey      锁的键
     * @param waitTime     等待锁的时间（秒）
     * @param leaseTime    锁持有时间（秒）
     * @param action       业务逻辑（Callable，有返回值）
     * @return 业务逻辑的返回值
     * @param <T> 返回值类型
     */
    public <T> T executeWithLock(String lockKey, int waitTime, int leaseTime, Callable<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = tryAcquireLock(lock, waitTime, leaseTime);
            if (!locked) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        Constants.LockMessage.ACQUIRE_FAILED);
            }

            log.debug("分布式锁获取成功: lockKey={}, waitTime={}s, leaseTime={}s",
                    lockKey, waitTime, leaseTime);
            return action.call();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("分布式锁操作被中断: lockKey={}", lockKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    Constants.LockMessage.OPERATION_INTERRUPTED);

        } catch (Exception e) {
            log.error("分布式锁执行业务逻辑异常: lockKey={}", lockKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    Constants.LockMessage.EXECUTION_ERROR + e.getMessage());

        } finally {
            releaseLock(lock, locked, lockKey);
        }
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lock       锁对象
     * @param waitTime   等待时间（秒）
     * @param leaseTime  持有时间（秒）
     * @return 是否成功获取锁
     * @throws InterruptedException 线程中断异常
     */
    private boolean tryAcquireLock(RLock lock, int waitTime, int leaseTime) 
            throws InterruptedException {
        return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
    }

    /**
     * 释放分布式锁
     *
     * @param lock     锁对象
     * @param locked   是否成功获取锁
     * @param lockKey  锁的键（用于日志）
     */
    private void releaseLock(RLock lock, boolean locked, String lockKey) {
        if (locked && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("分布式锁释放成功: lockKey={}", lockKey);
            } catch (Exception e) {
                log.error("分布式锁释放失败: lockKey={}", lockKey, e);
            }
        }
    }
}
