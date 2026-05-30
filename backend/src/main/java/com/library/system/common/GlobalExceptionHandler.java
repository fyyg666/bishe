package com.library.system.common;

import com.library.system.dto.ApiResponse;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 
 * <p>
 * 统一拦截并处理Controller层抛出的各类异常，将异常信息转换为标准的
 * {@link ApiResponse} 响应格式返回给前端。异常处理优先级从具体到通用：
 * </p>
 * <ol>
 *   <li>{@link ResourceNotFoundException} — 资源未找到（404）</li>
 *   <li>{@link UnauthorizedException} — 未授权（401）</li>
 *   <li>{@link ForbiddenException} — 禁止访问（403）</li>
 *   <li>{@link BusinessException} — 通用业务异常（由ErrorCode.httpStatus决定）</li>
 *   <li>{@link MethodArgumentNotValidException} — 请求参数校验失败（@Valid触发）</li>
 *   <li>{@link BindException} — 参数绑定异常</li>
 *   <li>{@link IllegalArgumentException} — 非法参数</li>
 *   <li>{@link RuntimeException} — 业务运行时异常</li>
 *   <li>{@link Exception} — 兜底处理所有未捕获异常</li>
 * </ol>
 * <p>
 * 所有异常均通过SLF4J记录日志，关联 {@link ErrorCode} 枚举以统一错误码规范，
 * 便于问题排查和前端统一错误处理。
 * </p>
 * <p>
 * FIXED: SEC-P3-01 统一错误消息格式，不向客户端暴露堆栈信息和内部异常细节，
 * 所有非业务异常统一返回通用错误消息，详细异常信息仅记录到服务端日志。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理资源未找到异常 
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("资源未找到 [{}]: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(e.getErrorCode(), e.getMessage(), request.getRequestURI());
    }

    /**
     * 处理未授权异常 
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("未授权 [{}]: {}", request.getRequestURI(), e.getMessage());
        
        return buildErrorResponse(e.getErrorCode(), "认证失败", request.getRequestURI());
    }

    /**
     * 处理禁止访问异常 
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("禁止访问 [{}]: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(e.getErrorCode(), "权限不足", request.getRequestURI());
    }

    /**
     * 处理业务异常 
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(e.getErrorCode(), e.getMessage(), request.getRequestURI());
    }

    /**
     * 处理运行时异常（兼容旧代码中直接 throw RuntimeException 的场景）
     * FIXED: SEC-P3-01 不向客户端暴露RuntimeException原始消息，统一返回通用错误
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "系统内部错误", request.getRequestURI()));
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常 [{}]: {}", request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.PARAMETER_ERROR, message, request.getRequestURI()));
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常 [{}]: {}", request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.PARAMETER_ERROR, message, request.getRequestURI()));
    }

    /**
     * 处理IllegalArgumentException
     * FIXED: SEC-P3-01 不向客户端暴露IllegalArgumentException原始消息
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("参数异常 [{}]: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.PARAMETER_ERROR, "参数错误", request.getRequestURI()));
    }

    /**
     * 处理所有未捕获的异常
     * FIXED: SEC-P3-01 不暴露任何内部异常信息，仅返回通用系统错误
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}]: ", request.getRequestURI(), e);
        // 不向客户端暴露异常消息和堆栈，仅返回通用错误
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试", request.getRequestURI()));
    }

    /**
     * 根据ErrorCode的httpStatus构建响应 
     *
     * @param errorCode 错误码枚举
     * @param message   错误消息
     * @param path      请求路径
     * @return ResponseEntity包含ApiResponse和对应的HTTP状态码
     */
    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(ErrorCode errorCode, String message, String path) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, message, path));
    }
}
