package com.library.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 统一请求日志记录 AOP
 * <p>
 * 拦截所有 Controller 方法，自动记录请求路径、参数和执行耗时。
 * 提供 tracing requestId 串联请求日志，方便答辩演示时排查问题。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 */
@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingAspect.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 切点：所有 Controller 下的 public 方法
     */
    @Pointcut("execution(public * com.library.system.controller.*.*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：记录请求参数和执行耗时
     */
    @Around("controllerMethods()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long startTime = System.currentTimeMillis();

        // 收集请求信息
        String methodName = joinPoint.getSignature().toShortString();
        String requestInfo = buildRequestInfo();

        // 记录请求开始
        if (log.isDebugEnabled()) {
            String args = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> !(arg instanceof HttpServletRequest)
                            && !(arg instanceof org.springframework.security.core.Authentication))
                    .map(this::safeToJson)
                    .collect(Collectors.joining(", "));
            log.debug("[req-{}] → {} | args=[{}] | {}", requestId, methodName,
                    args.length() > 200 ? args.substring(0, 200) + "..." : args, requestInfo);
        }

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;

            // 慢请求告警（超过2秒）
            if (elapsed > 2000) {
                log.warn("[req-{}] ← {} | 耗时={}ms (慢请求) | {}", requestId, methodName, elapsed, requestInfo);
            } else {
                log.info("[req-{}] ← {} | 耗时={}ms | {}", requestId, methodName, elapsed, requestInfo);
            }

            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[req-{}] ✗ {} | 耗时={}ms | 异常: {} | {}",
                    requestId, methodName, elapsed, e.getMessage(), requestInfo);
            throw e;
        }
    }

    /**
     * 构建请求信息字符串
     */
    private String buildRequestInfo() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "no-request-context";
            }
            HttpServletRequest request = attributes.getRequest();
            return request.getMethod() + " " + request.getRequestURI();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 安全地将参数转为JSON字符串，避免序列化异常
     */
    private String safeToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            // 跳过特殊对象（HttpServletRequest、Authentication等框架对象）
            if (obj instanceof HttpServletRequest
                    || obj instanceof Authentication
                    || obj.getClass().getName().contains("servlet")
                    || obj.getClass().getName().startsWith("org.springframework")) {
                return obj.getClass().getSimpleName();
            }
            String json = objectMapper.writeValueAsString(obj);
            return json.length() > 100 ? json.substring(0, 100) + "..." : json;
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
