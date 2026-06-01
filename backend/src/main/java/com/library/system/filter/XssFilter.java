package com.library.system.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * XSS防护过滤器
 * 使用Hutool的XssUtil.stripTags()对所有请求参数进行XSS过滤
 *
 * @author Security Team
 * @version 2.0.0
 */
@Slf4j
@Component
public class XssFilter extends OncePerRequestFilter {

    /**
     * 公开接口白名单 - 这些接口跳过XSS过滤（相对于context-path的路径）
     * FIXED: P2-013 移除/books、/seats等前缀匹配，改为精确路径避免白名单过宽
     */
    private static final Set<String> WHITELIST_PATHS = Set.of(
            "/actuator/health",
            "/actuator/info"
    );

    /**
     * XSS危险标签模式（用于增强检测）
     */
    private static final String[] DANGEROUS_PATTERNS = {
            "script", "javascript", "onerror", "onload", "onclick",
            "onmouseover", "onfocus", "onblur", "onchange", "onsubmit",
            "expression", "iframe", "object", "embed", "form", "input",
            "textarea", "link", "meta", "style"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 检查是否是白名单路径
        if (isWhitelisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 包装请求以进行XSS处理，直接使用原始response避免响应体截断
        XssRequestWrapper xssRequest = new XssRequestWrapper(request);

        filterChain.doFilter(xssRequest, response);
    }

    /**
     * 检查请求路径是否在白名单中
     * FIXED: P2-013 移除前缀匹配，仅使用精确匹配避免白名单过宽
     */
    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = path.substring(contextPath.length());

        // 仅精确匹配（移除了不安全的前缀匹配逻辑）
        return WHITELIST_PATHS.contains(relativePath);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS请求不进行XSS过滤
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    /**
     * XSS安全检查方法 - 检查字符串是否包含危险内容
     *
     * @param content 待检查的字符串
     * @return true表示安全，false表示包含危险内容
     */
    public static boolean isSafe(String content) {
        if (StrUtil.isBlank(content)) {
            return true;
        }

        String lowerContent = content.toLowerCase();
        for (String pattern : DANGEROUS_PATTERNS) {
            // 检查是否是事件处理器（on开头）或危险标签
            if (pattern.startsWith("on")) {
                if (lowerContent.contains("<" + pattern) || lowerContent.contains(" " + pattern)
                        || lowerContent.contains("\"" + pattern) || lowerContent.contains("'" + pattern)
                        || lowerContent.contains("=" + pattern)) {
                    return false;
                }
            } else if (pattern.equals("script")) {
                if (lowerContent.contains("<script") || lowerContent.contains("javascript:")
                        || lowerContent.contains("vbscript:")) {
                    return false;
                }
            } else {
                if (lowerContent.contains("<" + pattern)) {
                    return false;
                }
            }
        }
        return true;
    }
}
