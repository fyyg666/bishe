package com.library.system.exception;

import com.library.system.enums.ErrorCode;

/**
 * 未授权异常
 * <p>
 * 当用户未登录或 Token 无效/过期时抛出。
 * HTTP 状态码映射为 401 UNAUTHORIZED。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class UnauthorizedException extends BusinessException {

    /**
     * 基于 ErrorCode 构造
     *
     * @param errorCode 错误码枚举
     */
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 基于 ErrorCode + 自定义消息构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 便捷构造：使用 AUTH_FAILED + 自定义消息
     *
     * @param message 错误描述
     */
    public UnauthorizedException(String message) {
        super(ErrorCode.AUTH_FAILED, message);
    }
}
