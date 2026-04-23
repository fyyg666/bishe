package com.library.system.exception;

import com.library.system.enums.ErrorCode;

/**
 * 禁止访问异常
 * <p>
 * 当用户已认证但无足够权限执行操作时抛出。
 * HTTP 状态码映射为 403 FORBIDDEN。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ForbiddenException extends BusinessException {

    /**
     * 基于 ErrorCode 构造
     *
     * @param errorCode 错误码枚举
     */
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 基于 ErrorCode + 自定义消息构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 便捷构造：使用 INSUFFICIENT_PERMISSION + 自定义消息
     *
     * @param message 权限不足描述
     */
    public ForbiddenException(String message) {
        super(ErrorCode.INSUFFICIENT_PERMISSION, message);
    }
}
