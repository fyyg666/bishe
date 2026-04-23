package com.library.system.exception;

import com.library.system.enums.ErrorCode;

/**
 * 资源未找到异常
 * <p>
 * 当请求的业务资源（图书、读者、公告等）不存在或已被删除时抛出。
 * HTTP 状态码映射为 404 NOT FOUND。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 基于 ErrorCode 构造（应使用 RESOURCE_NOT_FOUND 或模块级 NOT_FOUND 枚举）
     *
     * @param errorCode 错误码枚举
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 基于 ErrorCode + 自定义消息构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息（如 "图书ID=123不存在"）
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 便捷构造：使用 RESOURCE_NOT_FOUND + 自定义消息
     *
     * @param message 资源描述消息
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
