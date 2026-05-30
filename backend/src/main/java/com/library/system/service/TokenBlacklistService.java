package com.library.system.service;

/**
 * Token黑名单服务接口
 */
public interface TokenBlacklistService {

    /**
     * 将Token加入黑名单
     * @param token Token
     * @param ttlSeconds 过期时间（秒）
     */
    void addToBlacklist(String token, long ttlSeconds);

    /**
     * 检查Token是否在黑名单中
     * @param token Token
     * @return true表示在黑名单中
     */
    boolean isBlacklisted(String token);
}
