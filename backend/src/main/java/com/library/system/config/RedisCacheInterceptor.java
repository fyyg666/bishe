package com.library.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.common.Constants;
import com.library.system.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "http:cache:";
    private static final long CACHE_TTL_MINUTES = 5;

    private static final Set<String> CACHEABLE_PATHS = Set.of(
            "/books",
            "/categories",
            "/statistics/overview",
            "/borrows/rules"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = extractPath(request);

        if (!isCacheablePath(path)) {
            return true;
        }

        if (isAdminRequest(request)) {
            return true;
        }

        String cacheKey = buildCacheKey(request);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(cached));
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取异常,降级到正常请求: key={}, error={}", cacheKey, e.getMessage());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (ex != null || !"GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        if (!(response instanceof ContentCachingResponseWrapper)) {
            return;
        }

        String path = extractPath(request);

        if (!isCacheablePath(path)) {
            return;
        }

        if (isAdminRequest(request)) {
            return;
        }

        if (response.getStatus() != 200) {
            return;
        }

        ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper) response;
        byte[] content = responseWrapper.getContentAsByteArray();
        if (content.length == 0) {
            return;
        }

        String cacheKey = buildCacheKey(request);
        try {
            String responseBody = new String(content, response.getCharacterEncoding());
            Object parsed = objectMapper.readValue(responseBody, Object.class);
            redisTemplate.opsForValue().set(cacheKey, parsed, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis缓存写入异常: key={}, error={}", cacheKey, e.getMessage());
        }
    }

    private boolean isCacheablePath(String path) {
        for (String cacheablePath : CACHEABLE_PATHS) {
            if (path.equals(cacheablePath) || path.startsWith(cacheablePath + "?")) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            String token = authHeader.substring(7);
            String role = jwtUtils.getRoleFromToken(token);
            return Constants.Role.ADMIN.equals(role);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        return requestURI.substring(contextPath.length());
    }

    private String buildCacheKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            return CACHE_PREFIX + uri + "?" + queryString;
        }
        return CACHE_PREFIX + uri;
    }
}
