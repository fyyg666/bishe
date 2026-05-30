package com.library.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 座位Redis缓存服务
 * Write-Through模式，Redis不可用时自动降级到DB
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Service
public class SeatRedisCache {

    private final StringRedisTemplate redisTemplate;

    @Value("${seat.cache.ttl-seconds:3600}")
    private int ttlSeconds;

    private static final String SEAT_STATUS_PREFIX = "seat:status:";

    public SeatRedisCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取座位状态（从Redis）
     */
    public String getSeatStatus(String seatNumber) {
        try {
            return redisTemplate.opsForValue().get(SEAT_STATUS_PREFIX + seatNumber);
        } catch (Exception e) {
            log.warn("座位缓存读取失败，降级到DB: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 更新座位状态（同步写入Redis + 通知DB层更新）
     */
    public void updateSeatStatus(String seatNumber, String status) {
        try {
            redisTemplate.opsForValue().set(
                    SEAT_STATUS_PREFIX + seatNumber,
                    status,
                    ttlSeconds,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("座位缓存写入失败，降级到DB: {}", e.getMessage());
        }
    }

    /**
     * 清除座位缓存
     */
    public void evictSeatStatus(String seatNumber) {
        try {
            redisTemplate.delete(SEAT_STATUS_PREFIX + seatNumber);
        } catch (Exception e) {
            log.warn("座位缓存清除失败: {}", e.getMessage());
        }
    }
}
