package com.library.system.exception;

import com.library.system.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类
 * <p>
 * 所有自定义业务异常的父类，关联 {@link ErrorCode} 枚举以统一错误码规范。
 * Service 层抛出此异常或其子类，由 {@link com.library.system.common.GlobalExceptionHandler} 统一拦截处理。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 关联的 ErrorCode 枚举 */
    private final ErrorCode errorCode;

    /**
     * 基于 ErrorCode 构造
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 基于 ErrorCode + 自定义消息构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 基于 ErrorCode + 原因异常构造
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 基于 ErrorCode + 自定义消息 + 原因异常构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
