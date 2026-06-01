package com.library.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.entity.OperationLog;
import com.library.system.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class RequestLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingAspect.class);

    private final ObjectMapper objectMapper;
    private final OperationLogMapper operationLogMapper;

    @Pointcut("execution(public * com.library.system.controller.*.*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long startTime = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().toShortString();
        String requestInfo = buildRequestInfo();

        String args = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> !(arg instanceof HttpServletRequest)
                        && !(arg instanceof Authentication))
                .map(this::safeToJson)
                .collect(Collectors.joining(", "));
        if (args.length() > 500) {
            args = args.substring(0, 500) + "...";
        }

        if (log.isDebugEnabled()) {
            log.debug("[req-{}] → {} | args=[{}] | {}", requestId, methodName,
                    args.length() > 200 ? args.substring(0, 200) + "..." : args, requestInfo);
        }

        HttpServletRequest httpRequest = getCurrentRequest();
        String module = extractModule(joinPoint);
        String operation = extractOperation(joinPoint);
        String ip = httpRequest != null ? getClientIp(httpRequest) : null;
        UserInfo userInfo = getCurrentUser();

        Object result;
        String resultStr = null;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed > 2000) {
                log.warn("[req-{}] ← {} | 耗时={}ms (慢请求) | {}", requestId, methodName, elapsed, requestInfo);
            } else {
                log.info("[req-{}] ← {} | 耗时={}ms | {}", requestId, methodName, elapsed, requestInfo);
            }

            resultStr = safeToJson(result);
            if (resultStr != null && resultStr.length() > 500) {
                resultStr = resultStr.substring(0, 500) + "...";
            }

            asyncSaveLog(module, operation, methodName, args, resultStr,
                    userInfo.userId, userInfo.username, ip, elapsed, null);

            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[req-{}] ✗ {} | 耗时={}ms | 异常: {} | {}",
                    requestId, methodName, elapsed, e.getMessage(), requestInfo);

            errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }

            asyncSaveLog(module, operation, methodName, args, errorMsg,
                    userInfo.userId, userInfo.username, ip, elapsed, e.getClass().getSimpleName());

            throw e;
        }
    }

    @Async("asyncExecutor")
    protected void asyncSaveLog(String module, String operation, String method, String params,
                                String result, Long userId, String username, String ip,
                                Long duration, String errorType) {
        try {
            OperationLog opLog = OperationLog.builder()
                    .module(module)
                    .operation(operation)
                    .method(method)
                    .params(params)
                    .result(result)
                    .userId(userId)
                    .username(username)
                    .ip(ip)
                    .duration(duration)
                    .build();
            operationLogMapper.insert(opLog);
        } catch (Exception e) {
            log.warn("操作日志持久化失败: {}", e.getMessage());
        }
    }

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

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractModule(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return simpleName.replace("Controller", "").toUpperCase();
    }

    private String extractOperation(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }

    private String getClientIp(HttpServletRequest request) {
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
        return ip;
    }

    private UserInfo getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                return new UserInfo(null, username);
            }
        } catch (Exception ignored) {
        }
        return new UserInfo(null, null);
    }

    private String safeToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
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

    private record UserInfo(Long userId, String username) {}
}
