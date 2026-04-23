package com.library.system.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 安全审计日志服务
 * <p>
 * 独立于业务操作日志的安全事件记录器，使用专用的SECURITY_AUDIT Logger
 * 输出到独立的 security-audit.log 文件。
 * </p>
 * <p>
 * 记录的安全事件类型：
 * <ul>
 *   <li>登录失败 — 认证异常、密码错误、账户锁定</li>
 *   <li>权限不足 — 越权访问、角色校验失败</li>
 *   <li>异常访问 — XSS攻击检测、SQL注入检测、频率超限</li>
 *   <li>Token安全 — Token无效、Token过期、Token被吊销</li>
 *   <li>数据安全 — 敏感数据访问、批量导出操作</li>
 * </ul>
 * </p>
 *
 * FIXED: SEC-P3-02 独立security-audit.log记录安全事件
 *
 * @author Security Team
 * @version 2.0.0
 */
@Slf4j
@Component
public class SecurityAuditLogger {

    /** 安全审计专用Logger，通过logback配置输出到独立文件 */
    private static final org.slf4j.Logger SECURITY_LOG =
            org.slf4j.LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * 记录登录失败事件
     *
     * @param username 尝试登录的用户名
     * @param reason   失败原因
     */
    public void logLoginFailure(String username, String reason) {
        String ip = getClientIp();
        SECURITY_LOG.warn("[LOGIN_FAILURE] username={}, ip={}, reason={}", username, ip, reason);
    }

    /**
     * 记录权限不足事件
     *
     * @param userId     用户ID
     * @param resource   访问的资源路径
     * @param requiredRole 需要的角色
     */
    public void logAccessDenied(Long userId, String resource, String requiredRole) {
        String ip = getClientIp();
        SECURITY_LOG.warn("[ACCESS_DENIED] userId={}, resource={}, requiredRole={}, ip={}",
                userId, resource, requiredRole, ip);
    }

    /**
     * 记录Token安全事件
     *
     * @param eventType 事件类型（TOKEN_INVALID, TOKEN_EXPIRED, TOKEN_REVOKED）
     * @param detail    详细信息
     */
    public void logTokenSecurityEvent(String eventType, String detail) {
        String ip = getClientIp();
        SECURITY_LOG.warn("[{}] detail={}, ip={}", eventType, detail, ip);
    }

    /**
     * 记录XSS攻击检测事件
     *
     * @param path     请求路径
     * @param param    受影响的参数
     * @param value    检测到的危险值（脱敏后截断）
     */
    public void logXssDetection(String path, String param, String value) {
        String ip = getClientIp();
        String truncatedValue = value != null && value.length() > 100
                ? value.substring(0, 100) + "...(truncated)" : value;
        SECURITY_LOG.warn("[XSS_DETECTED] path={}, param={}, value={}, ip={}",
                path, param, truncatedValue, ip);
    }

    /**
     * 记录频率限制触发事件
     *
     * @param clientId 客户端标识
     * @param uri      请求URI
     * @param count    当前计数
     * @param limit    限制阈值
     */
    public void logRateLimitExceeded(String clientId, String uri, long count, int limit) {
        SECURITY_LOG.warn("[RATE_LIMIT] clientId={}, uri={}, count={}/{}",
                clientId, uri, count, limit);
    }

    /**
     * 记录账户锁定事件
     *
     * @param username 被锁定的用户名
     * @param duration 锁定时长描述
     */
    public void logAccountLocked(String username, String duration) {
        String ip = getClientIp();
        SECURITY_LOG.warn("[ACCOUNT_LOCKED] username={}, duration={}, ip={}", username, duration, ip);
    }

    /**
     * 记录批量数据导出事件
     *
     * @param userId    操作用户ID
     * @param dataType  导出的数据类型
     * @param recordCount 导出记录数量
     */
    public void logDataExport(Long userId, String dataType, int recordCount) {
        String ip = getClientIp();
        SECURITY_LOG.info("[DATA_EXPORT] userId={}, dataType={}, recordCount={}, ip={}",
                userId, dataType, recordCount, ip);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
