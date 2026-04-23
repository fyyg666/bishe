package com.library.system.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.ApiResponse;
import com.library.system.security.SecurityAuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * API限流过滤器
 * 基于Redis + Lua脚本实现滑动窗口限流算法
 *
 * FIXED: P2-006 从固定窗口计数器改为真正的滑动窗口算法
 * 使用Redis ZSET存储请求时间戳，通过Lua脚本保证原子性，
 * 精确统计滑动窗口内的请求数量，避免固定窗口的边界突发问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger securityAuditLogger;

    @Value("${rate-limiter.enabled:true}")
    private boolean enabled;

    @Value("${rate-limiter.default-limit:60}")
    private int defaultLimit;

    @Value("${rate-limiter.window-size:60}")
    private int windowSize;

    @Value("${rate-limiter.redis-key-prefix:rate:}")
    private String rateLimitPrefix;

    /**
     * FIXED: SEC-P2-02 登录接口独立限流配置（5次/分钟）
     */
    @Value("${rate-limiter.login-limit:5}")
    private int loginLimit;

    /** 敏感接口路径（需要独立严格限流） */
    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/auth/login",
            "/auth/register"
    );

    /** 登录限流key前缀 */
    private static final String LOGIN_RATE_PREFIX = "login_rate:";

    /**
     * Lua脚本: 滑动窗口限流
     * KEYS[1] = 限流key
     * ARGV[1] = 当前时间戳(毫秒)
     * ARGV[2] = 窗口大小(毫秒)
     * ARGV[3] = 限流阈值
     *
     * 逻辑:
     * 1. 移除窗口之外的旧记录
     * 2. 添加当前请求
     * 3. 统计窗口内请求数量
     * 4. 设置key过期时间(防止内存泄漏)
     * 5. 返回当前窗口内请求数和是否被限流
     */
    private static final String SLIDING_WINDOW_LUA_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            
            -- 移除窗口之外的旧记录
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            
            -- 添加当前请求
            redis.call('ZADD', key, now, now .. ':' .. math.random(1000000))
            
            -- 设置过期时间(窗口大小 + 1秒缓冲)
            redis.call('EXPIRE', key, math.ceil(window / 1000) + 1)
            
            -- 统计窗口内请求数量
            local count = redis.call('ZCARD', key)
            
            return {count, limit}
            """;

    private final DefaultRedisScript<List> slidingWindowScript = new DefaultRedisScript<>();

    public RateLimitFilter() {
        slidingWindowScript.setScriptText(SLIDING_WINDOW_LUA_SCRIPT);
        slidingWindowScript.setResultType(List.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String relativePath = getRelativePath(request);
        if (SENSITIVE_PATHS.contains(relativePath)) {
            if (!checkLoginRateLimit(request, response)) {
                return; // 登录限流触发，直接返回
            }
        }

        // 通用API限流检查
        String clientId = getClientIdentifier(request);
        String key = rateLimitPrefix + clientId + ":" + request.getRequestURI();

        try {
            long now = System.currentTimeMillis();
            long windowMillis = windowSize * 1000L;

            // 通过Lua脚本原子性执行滑动窗口限流
            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    slidingWindowScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowMillis),
                    String.valueOf(defaultLimit)
            );

            if (result != null && result.size() >= 2) {
                long currentCount = ((Number) result.get(0)).longValue();
                int limit = ((Number) result.get(1)).intValue();

                if (currentCount > limit) {
                    log.warn("滑动窗口限流触发: clientId={}, uri={}, count={}/{}",
                            clientId, request.getRequestURI(), currentCount, limit);
                    
                    securityAuditLogger.logRateLimitExceeded(clientId,
                            request.getRequestURI(), currentCount, limit);

                    response.setStatus(429);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                    response.setHeader("X-RateLimit-Remaining", "0");
                    response.setHeader("Retry-After", String.valueOf(windowSize));

                    ApiResponse<Void> apiResponse = ApiResponse.error(
                            429, "请求过于频繁，请稍后再试");
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                    return;
                }

                // 添加限流响应头
                response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                response.setHeader("X-RateLimit-Remaining",
                        String.valueOf(Math.max(0, limit - currentCount)));
            }

        } catch (Exception e) {
            log.error("滑动窗口限流检查异常: {}", e.getMessage());
            // 限流异常时降级放行
        }

        filterChain.doFilter(request, response);
    }

    /**
     * FIXED: SEC-P2-02 登录接口独立限流检查（5次/分钟）
     * 登录接口使用独立的Redis key和更严格的限流阈值
     *
     * @return true=通过限流，false=被限流
     */
    private boolean checkLoginRateLimit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientId = getClientIp(request);
        String key = LOGIN_RATE_PREFIX + clientId;

        try {
            long now = System.currentTimeMillis();
            long windowMillis = windowSize * 1000L;

            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    slidingWindowScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowMillis),
                    String.valueOf(loginLimit)
            );

            if (result != null && result.size() >= 2) {
                long currentCount = ((Number) result.get(0)).longValue();
                int limit = ((Number) result.get(1)).intValue();

                if (currentCount > limit) {
                    log.warn("登录接口限流触发: ip={}, count={}/{}", clientId, currentCount, limit);
                    
                    securityAuditLogger.logRateLimitExceeded("ip:" + clientId,
                            "/auth/login", currentCount, limit);

                    response.setStatus(429);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                    response.setHeader("X-RateLimit-Remaining", "0");
                    response.setHeader("Retry-After", String.valueOf(windowSize));

                    ApiResponse<Void> apiResponse = ApiResponse.error(
                            429, "登录尝试过于频繁，请稍后再试");
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                    return false;
                }

                response.setHeader("X-RateLimit-Limit-Login", String.valueOf(limit));
                response.setHeader("X-RateLimit-Remaining-Login",
                        String.valueOf(Math.max(0, limit - currentCount)));
            }

        } catch (Exception e) {
            log.error("登录限流检查异常: {}", e.getMessage());
            // 限流异常时降级放行
        }

        return true;
    }

    /**
     * 获取相对于context-path的请求路径
     */
    private String getRelativePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    /**
     * 获取客户端标识
     */
    private String getClientIdentifier(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }

        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health")
                || path.contains("/actuator/info")
                || path.contains("/actuator/prometheus");
    }
}
