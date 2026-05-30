package com.library.system.service;

/**
 * 账户锁定服务接口
 * FIXED: SEC-002 使用Redis进行分布式账户锁定
 * 
 * @author Security Team
 * @version 2.0.0
 */
public interface AccountLockService {

    /**
     * 账户锁定配置常量
     */
    int MAX_LOGIN_FAILURES = 5;           // 最大登录失败次数
    int LOCK_DURATION_MINUTES = 15;       // 锁定时长（分钟）
    String LOCK_PREFIX = "account:lock:";  // Redis锁定key前缀

    /**
     * 检查账户是否被锁定
     * 
     * @param userId 用户ID
     * @return true-已锁定，false-未锁定
     */
    boolean isLocked(Long userId);

    /**
     * 检查账户是否被锁定（基于用户名）
     * 
     * @param username 用户名
     * @return true-已锁定，false-未锁定
     */
    boolean isLockedByUsername(String username);

    /**
     * 记录登录失败次数
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @return 当前失败次数
     */
    int recordLoginFailure(Long userId, String username);

    /**
     * 清除登录失败次数
     * 
     * @param userId 用户ID
     */
    void clearLoginFailures(Long userId);

    /**
     * 获取剩余锁定时间（秒）
     * 
     * @param userId 用户ID
     * @return 剩余锁定时间，0表示未锁定
     */
    long getRemainingLockSeconds(Long userId);

    /**
     * 手动解锁账户（管理员操作）
     * 
     * @param userId 用户ID
     */
    void unlockAccount(Long userId);

    /**
     * 获取登录失败次数
     * 
     * @param userId 用户ID
     * @return 失败次数
     */
    int getLoginFailCount(Long userId);
}
