package com.library.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.annotation.AuditLog;
import com.library.system.common.Constants;
import com.library.system.config.AsyncConfig;
import com.library.system.entity.OperationLog;
import com.library.system.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面
 * <p>
 * 基于 AOP 的操作日志记录切面，拦截标注了 {@link AuditLog} 注解的Controller方法，
 * 自动采集操作模块、操作类型、请求参数、返回结果、用户信息、IP地址和执行时长等，
 * 并持久化到 {@code sys_operation_log} 表中。
 * </p>
 *
 * <p>采集策略：</p>
 * <ul>
 *   <li>请求参数：通过方法签名获取参数名和参数值，序列化为JSON</li>
 *   <li>返回结果：截取前2000字符，避免存储过大数据</li>
 *   <li>用户信息：优先从Spring Security上下文获取，降级为"anonymous"</li>
 *   <li>IP地址：通过 {@code X-Forwarded-For} 或 {@code X-Real-IP} 请求头获取真实IP</li>
 *   <li>执行时长：精确到毫秒</li>
 * </ul>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 * @see AuditLog
 * @see OperationLog
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;

    /** 返回结果最大记录长度 */
    private static final int MAX_RESULT_LENGTH = 2000;

    /**
     * 需要脱敏的参数名模式（不区分大小写匹配）
     * FIXED: P3-007 审计日志密码脱敏
     */
    private static final java.util.Set<String> SENSITIVE_PARAM_NAMES = java.util.Set.of(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "secret", "token", "accesstoken", "refreshtoken"
    );

    /** 脱敏替换文本 */
    private static final String MASKED_VALUE = "\"******\"";

    /** JSON序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 环绕通知：拦截所有标注了 @AuditLog 的方法
     * <p>
     * 执行流程：
     * <ol>
     *   <li>记录方法开始时间</li>
     *   <li>采集请求参数和上下文信息（用户、IP等）</li>
     *   <li>执行目标方法</li>
     *   <li>计算执行时长，采集返回结果</li>
     *   <li>异步写入操作日志到数据库</li>
     * </ol>
     * </p>
     *
     * @param joinPoint 切点信息
     * @return 目标方法的返回值
     * @throws Throwable 目标方法可能抛出的异常
     */
    @Around("@annotation(com.library.system.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 采集方法信息
        String className = joinPoint.getTarget().getClass().getName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String method = className + "." + methodName;

        // 获取 @AuditLog 注解
        AuditLog auditLog = signature.getMethod().getAnnotation(AuditLog.class);

        // 采集请求参数
        String params = getMethodParams(joinPoint);

        // 采集用户信息
        Long userId = getUserId();
        String username = getUsername();

        // 采集IP地址
        String ip = getClientIp();

        Object result;
        try {
            // 执行目标方法
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 方法执行异常时也记录日志
            log.warn("操作日志记录异常方法: {}, 异常: {}", method, e.getMessage());
            throw e;
        }

        // 计算执行时长
        long duration = System.currentTimeMillis() - startTime;

        // 采集返回结果
        String resultStr = getReturnValue(result);

        // 构建操作日志实体
        OperationLog operationLog = OperationLog.builder()
                .module(auditLog.module())
                .operation(auditLog.operation())
                .method(method)
                .params(params)
                .result(resultStr)
                .userId(userId)
                .username(username)
                .ip(ip)
                .duration(duration)
                .createTime(LocalDateTime.now())
                .build();

        saveLogAsync(operationLog);

        return result;
    }

    /**
     * 获取方法参数的JSON字符串
     *
     * @param joinPoint 切点信息
     * @return 参数JSON字符串，获取失败时返回空字符串
     */
    private String getMethodParams(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (paramNames == null || paramNames.length == 0) {
                return "{}";
            }

            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < paramNames.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(paramNames[i]).append("\": ");
                if (args[i] != null) {
                    // 避免序列化HttpServletRequest等不可序列化对象
                    if (args[i] instanceof HttpServletRequest ||
                        args[i] instanceof Authentication) {
                        sb.append("\"[ignored]\"");
                    } else if (isSensitiveParam(paramNames[i])) {
                        
                        sb.append(MASKED_VALUE);
                    } else {
                        try {
                            String value = objectMapper.writeValueAsString(args[i]);
                            // 对包含敏感字段的JSON对象进行脱敏
                            value = maskSensitiveFields(value);
                            // 参数值过长时截断
                            if (value.length() > 1000) {
                                value = value.substring(0, 1000) + "...(truncated)";
                            }
                            sb.append(value);
                        } catch (Exception e) {
                            sb.append("\"").append(args[i].toString()).append("\"");
                        }
                    }
                } else {
                    sb.append("null");
                }
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取方法参数失败: {}", e.getMessage()); 
            return "{}";
        }
    }

    /**
     * 检查参数名是否为敏感字段
     * FIXED: P3-007 审计日志密码脱敏
     */
    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        return SENSITIVE_PARAM_NAMES.contains(paramName.toLowerCase());
    }

    /**
     * 对JSON字符串中的敏感字段值进行脱敏
     * FIXED: P3-007 密码字段输出******
     */
    private String maskSensitiveFields(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String result = json;
        for (String field : SENSITIVE_PARAM_NAMES) {
            // 匹配 "password":"value" 或 "password": "value" 并替换值为 ******
            result = result.replaceAll(
                    "(\""+ field + "\"\\s*:\\s*)\"[^\"]*\"",
                    "$1\"******\""
            );
        }
        return result;
    }

    /**
     * 获取返回值的字符串表示
     * <p>
     * 对返回值进行JSON序列化，并截取前 {@link #MAX_RESULT_LENGTH} 个字符，
     * 避免存储过大的返回数据（如分页列表）。
     * </p>
     *
     * @param result 方法返回值
     * @return 返回值字符串
     */
    private String getReturnValue(Object result) {
        if (result == null) {
            return "null";
        }
        try {
            String value = objectMapper.writeValueAsString(result);
            if (value.length() > MAX_RESULT_LENGTH) {
                return value.substring(0, MAX_RESULT_LENGTH) + "...(truncated)";
            }
            return value;
        } catch (Exception e) {
            return result.toString();
        }
    }

    /**
     * 从Spring Security上下文中获取当前用户ID
     *
     * @return 用户ID，未登录时返回null
     */
    private Long getUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !Constants.Token.ANONYMOUS_USER.equals(authentication.getPrincipal())) { 
                String principal = authentication.getName();
                try {
                    return Long.valueOf(principal);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } catch (Exception e) {
            log.debug("获取用户ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从Spring Security上下文中获取当前用户名
     *
     * @return 用户名，未登录时返回"anonymous"
     */
    private String getUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取用户名失败: {}", e.getMessage());
        }
        return "anonymous";
    }

    /**
     * 获取客户端真实IP地址
     * <p>
     * 依次检查 {@code X-Forwarded-For}、{@code X-Real-IP} 请求头和
     * {@code request.getRemoteAddr()}，优先返回代理转发的真实IP。
     * </p>
     *
     * @return 客户端IP地址，获取失败时返回"unknown"
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // 多级代理时取第一个IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            log.debug("获取客户端IP失败: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 异步保存操作日志到数据库 
     * <p>
     * 使用 {@link AsyncConfig#OPERATION_LOG_EXECUTOR} 线程池异步执行数据库写入，
     * 避免日志持久化阻塞主业务线程。线程池满时由 CallerRunsPolicy 兜底，
     * 不会丢失日志记录。
     * </p>
     *
     * @param operationLog 操作日志实体
     */
    @Async(AsyncConfig.OPERATION_LOG_EXECUTOR)
    public void saveLogAsync(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("异步保存操作日志失败: module={}, operation={}, error={}",
                    operationLog.getModule(), operationLog.getOperation(), e.getMessage());
        }
    }
}
