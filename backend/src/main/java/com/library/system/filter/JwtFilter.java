package com.library.system.filter;

import com.library.system.common.Constants;
import com.library.system.security.SecurityAuditLogger;
import com.library.system.service.TokenBlacklistService;
import com.library.system.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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
    private final TokenBlacklistService tokenBlacklistService;
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
                response.getWriter().flush();
                return;
            }

            // 校验Token类型 — 仅允许Access Token通过
            String tokenType = jwtUtils.getTokenType(token);
            if (!Constants.Token.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                log.warn("Token类型不匹配: expected=ACCESS, actual={}, uri={}",
                        tokenType, request.getRequestURI());
                securityAuditLogger.logTokenSecurityEvent("TOKEN_TYPE_MISMATCH",
                        "uri=" + request.getRequestURI() + ", type=" + tokenType);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Token类型无效\"}");
                response.getWriter().flush();
                return;
            }

            // 检查Token是否在黑名单中
            String jti = jwtUtils.getJtiFromToken(token);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
            if (tokenBlacklistService.isBlacklisted(blacklistKey)) {
                log.warn("Token已被吊销: {}", request.getRequestURI());
                
                securityAuditLogger.logTokenSecurityEvent("TOKEN_REVOKED",
                        "uri=" + request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Token已被吊销\"}");
                response.getWriter().flush();
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
                            username,  // principal使用用户名
                            null,
                            Collections.singletonList(authority)
                    );
            authentication.setDetails(userId);  // userId通过details传递

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
            response.getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** AntPathMatcher 用于精确路径匹配，防止路径绕过漏洞 */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 公开接口路径模式列表（不需要JWT认证） */
    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/doc.html",
            "/favicon.ico"
    };

    /** 公开GET接口路径模式列表 */
    private static final String[] PUBLIC_GET_PATHS = {
            "/books",
            "/books/hot",
            "/books/**",
            "/books/check-isbn",
            "/categories",
            "/seats",
            "/seats/check-availability"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // FIXED: 去掉 context-path 后再匹配，避免 /api/v1 + /auth/login 无法匹配 /auth/login
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        // 确保路径以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String method = request.getMethod();

        // 1. 检查是否命中公开接口（所有HTTP方法）
        for (String pattern : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }

        // 2. 检查是否命中公开GET接口
        if ("GET".equalsIgnoreCase(method)) {
            for (String pattern : PUBLIC_GET_PATHS) {
                if (PATH_MATCHER.match(pattern, path)) {
                    return true;
                }
            }
        }

        return false;
    }
}
