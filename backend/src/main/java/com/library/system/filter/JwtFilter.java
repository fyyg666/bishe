package com.library.system.filter;

import com.library.system.common.Constants;
import com.library.system.security.SecurityAuditLogger;
import com.library.system.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 拦截请求并验证JWT Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final SecurityAuditLogger securityAuditLogger;

    // Token黑名单前缀 
    private static final String TOKEN_BLACKLIST_PREFIX = Constants.Token.BLACKLIST_PREFIX;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");

        // 如果没有Token或格式不正确，直接放行（后续由Spring Security处理）
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(Constants.Token.BEARER_PREFIX)) { 
            filterChain.doFilter(request, response);
            return;
        }

        // 提取Token
        String token = authHeader.substring(Constants.Token.BEARER_PREFIX.length()); 

        try {
            // 验证Token
            if (!jwtUtils.validateToken(token)) {
                log.warn("Token验证失败: {}", request.getRequestURI());
                
                securityAuditLogger.logTokenSecurityEvent("TOKEN_INVALID",
                        "uri=" + request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\"}");
                return;
            }

            // 检查Token是否在黑名单中
            String jti = jwtUtils.getJtiFromToken(token);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                log.warn("Token已被吊销: {}", request.getRequestURI());
                
                securityAuditLogger.logTokenSecurityEvent("TOKEN_REVOKED",
                        "uri=" + request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Token已被吊销\"}");
                return;
            }

            // 获取用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            String role = jwtUtils.getRoleFromToken(token);

            // 构建认证对象
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(Constants.Token.ROLE_PREFIX + role); 
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),  // principal使用用户ID
                            null,
                            Collections.singletonList(authority)
                    );

            // 设置认证信息到Security上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Token验证成功: userId={}, username={}, uri={}",
                    userId, username, request.getRequestURI());

        } catch (Exception e) {
            log.warn("Token处理异常: {}", e.getMessage()); 
            
            securityAuditLogger.logTokenSecurityEvent("TOKEN_ERROR",
                    "uri=" + request.getRequestURI() + ", error=" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Token处理异常\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 登录、注册等公开接口不需要验证Token
        String path = request.getRequestURI();
        return path.contains("/auth/login")
                || path.contains("/auth/register")
                || path.contains("/auth/refresh")
                || path.contains("/actuator")
                || (path.contains("/books") && "GET".equalsIgnoreCase(request.getMethod()));
    }
}
