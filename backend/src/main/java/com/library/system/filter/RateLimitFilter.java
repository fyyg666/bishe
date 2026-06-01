package com.library.system.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.ApiResponse;
import com.library.system.security.SecurityAuditLogger;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.context.annotation.Profile;
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
@Profile("!no-redis")
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger securityAuditLogger;

    @Value("${rate-limiter.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    /** 标记Redis是否在启动时可用，避免每次限流检查都重复输出警告 */
    private volatile boolean redisAvailable = true;

    /** Redis不可用时的本地限流降级，基于Semaphore限制最大并发请求数 */
    private final java.util.concurrent.Semaphore localRateLimiter = new java.util.concurrent.Semaphore(100);

    /**
     * 启动时检查Redis连接可用性
     */
    @PostConstruct
    public void checkRedisAvailable() {
        try {
            redisTemplate.opsForValue().get("_rate_limit_health_check_");
            redisAvailable = true;
            log.info("RateLimitFilter: Redis连接正常，限流功能已启用");
        } catch (Exception e) {
            redisAvailable = false;
            log.error("RateLimitFilter: Redis连接失败！限流功能将降级为本地限流模式。请检查Redis服务。错误: {}", e.getMessage());
        }
    }

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

    /** 注册接口独立限流配置（3次/分钟，比登录更严格） */
    @Value("${rate-limiter.register-limit:3}")
    private int registerLimit;

    /** 敏感接口路径（需要独立严格限流） */
    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/auth/login"
    );

    /** 高消耗接口路径（10分钟10次） */
    private static final Set<String> HEAVY_PATHS = Set.of(
            "/statistics/export",
            "/statistics/monthly",
            "/books/export",
            "/borrows/export"
    );

    /** 登录限流key前缀 */
    private static final String LOGIN_RATE_PREFIX = "login_rate:";

    /** 注册限流key前缀 */
    private static final String REGISTER_RATE_PREFIX = "register_rate:";

    /** 高消耗接口限流key前缀 */
    private static final String HEAVY_RATE_PREFIX = "heavy_rate:";

    @Value("${rate-limiter.heavy-limit:10}")
    private int heavyLimit;

    @Value("${rate-limiter.heavy-window-seconds:600}")
    private int heavyWindowSeconds;

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

    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> slidingWindowScript = new DefaultRedisScript<>();

    {
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

        // 注册接口独立限流（3次/分钟，比登录更严格）
        if ("/auth/register".equals(relativePath)) {
            if (!checkRegisterRateLimit(request, response)) {
                return;
            }
        }

        if (SENSITIVE_PATHS.contains(relativePath)) {
            if (!checkLoginRateLimit(request, response)) {
                return; // 登录限流触发，直接返回
            }
        }

        // 高消耗接口独立限流（每10分钟10次）
        if (HEAVY_PATHS.contains(relativePath)) {
            String clientId = getClientIp(request);
            String key = HEAVY_RATE_PREFIX + clientId + ":" + relativePath;
            if (!checkHeavyPathRateLimit(request, response, key)) {
                return;
            }
        }

        // 通用API限流检查
        String clientId = getClientIdentifier(request);
        String key = rateLimitPrefix + clientId + ":" + request.getRequestURI();
        boolean localFallbackAcquired = false;

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

        } catch (RuntimeException | IOException e) {
            if (redisAvailable) {
                redisAvailable = false;
                log.error("滑动窗口限流检查异常(Redis不可用)，降级为本地限流: {}", e.getMessage());
            }
            if (!localRateLimiter.tryAcquire()) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                ApiResponse<Void> apiResponse = ApiResponse.error(429, "系统繁忙，请稍后再试");
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
            localFallbackAcquired = true;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (localFallbackAcquired) {
                localRateLimiter.release();
            }
        }
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

        } catch (RuntimeException | IOException e) {
            log.error("登录限流检查异常: {}", e.getMessage());
            if (!localRateLimiter.tryAcquire()) {
                log.warn("登录限流降级触发(系统繁忙): ip={}", clientId);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", String.valueOf(windowSize));
                ApiResponse<Void> apiResponse = ApiResponse.error(429, "系统繁忙，请稍后再试");
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return false;
            }
            // semaphore会在调用方filterChain执行后统一释放，此处不提前释放
        }

        return true;
    }

    /**
     * 注册接口独立限流检查（3次/分钟）
     * 注册接口使用独立的Redis key和更严格的限流阈值，防止恶意批量注册
     *
     * @return true=通过限流，false=被限流
     */
    private boolean checkRegisterRateLimit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientId = getClientIp(request);
        String key = REGISTER_RATE_PREFIX + clientId;

        try {
            long now = System.currentTimeMillis();
            long windowMillis = windowSize * 1000L;

            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    slidingWindowScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowMillis),
                    String.valueOf(registerLimit)
            );

            if (result != null && result.size() >= 2) {
                long currentCount = ((Number) result.get(0)).longValue();
                int limit = ((Number) result.get(1)).intValue();

                if (currentCount > limit) {
                    log.warn("注册接口限流触发: ip={}, count={}/{}", clientId, currentCount, limit);

                    securityAuditLogger.logRateLimitExceeded("ip:" + clientId,
                            "/auth/register", currentCount, limit);

                    response.setStatus(429);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                    response.setHeader("X-RateLimit-Remaining", "0");
                    response.setHeader("Retry-After", String.valueOf(windowSize));

                    ApiResponse<Void> apiResponse = ApiResponse.error(
                            429, "注册请求过于频繁，请稍后再试");
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                    return false;
                }

                response.setHeader("X-RateLimit-Limit-Register", String.valueOf(limit));
                response.setHeader("X-RateLimit-Remaining-Register",
                        String.valueOf(Math.max(0, limit - currentCount)));
            }

        } catch (RuntimeException | IOException e) {
            log.error("注册限流检查异常: {}", e.getMessage());
            if (!localRateLimiter.tryAcquire()) {
                log.warn("注册限流降级触发(系统繁忙): ip={}", clientId);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", String.valueOf(windowSize));
                ApiResponse<Void> apiResponse = ApiResponse.error(429, "系统繁忙，请稍后再试");
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return false;
            }
            // semaphore会在调用方filterChain执行后统一释放，此处不提前释放
        }

        return true;
    }

    /**
     * 高消耗接口独立限流检查（10分钟10次）
     */
    private boolean checkHeavyPathRateLimit(HttpServletRequest request, HttpServletResponse response, String key) throws IOException {
        try {
            long now = System.currentTimeMillis();
            long windowMillis = heavyWindowSeconds * 1000L;

            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    slidingWindowScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowMillis),
                    String.valueOf(heavyLimit));

            if (result != null && result.size() >= 2) {
                long currentCount = ((Number) result.get(0)).longValue();
                if (currentCount > heavyLimit) {
                    log.warn("高消耗接口限流触发: uri={}, count={}/{}",
                            request.getRequestURI(), currentCount, heavyLimit);

                    response.setStatus(429);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("X-RateLimit-Limit", String.valueOf(heavyLimit));
                    response.setHeader("X-RateLimit-Remaining", "0");
                    response.setHeader("Retry-After", String.valueOf(heavyWindowSeconds));

                    ApiResponse<Void> apiResponse = ApiResponse.error(
                            429, "该接口调用频率过高，请稍后再试");
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                    return false;
                }
            }
        } catch (RuntimeException | IOException e) {
            log.warn("高消耗接口限流检查异常，降级放行: {}", e.getMessage());
            if (!localRateLimiter.tryAcquire()) {
                log.warn("高消耗接口限流降级触发(系统繁忙): uri={}", request.getRequestURI());
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", String.valueOf(heavyWindowSeconds));
                ApiResponse<Void> apiResponse = ApiResponse.error(429, "系统繁忙，请稍后再试");
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return false;
            }
            localRateLimiter.release();
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
        if (!trustProxyHeaders) {
            return request.getRemoteAddr();
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            if (ip.contains(",")) {
                String[] ips = ip.split(",");
                ip = ips[0].trim();  // FIXED: 取第一个IP（真实客户端），而非最后一个（代理IP）
            }
            return ip;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health")
                || path.contains("/actuator/info")
                || path.contains("/actuator/prometheus");
    }
}
