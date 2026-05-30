package com.library.system.dto;

import com.library.system.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.UUID;

/**
 * 统一API响应DTO
 * 所有Controller接口均返回此格式，确保前后端响应结构一致
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 响应码：0-成功，其他-失败
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求追踪ID（UUID），用于日志排查与链路追踪
     */
    private String requestId;

    /**
     * 业务错误码，关联 ErrorCode 枚举
     */
    private Integer errorCode;

    /**
     * 服务器时间戳（epoch毫秒）
     */
    private Long timestamp;

    /**
     * 请求路径
     */
    private String path;

    // ========== 成功响应 ==========

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return success("success", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(0)
                .message(message)
                .data(data)
                .requestId(generateRequestId())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ========== 错误响应（errorCode + code） ==========

    /**
     * 错误响应（基于 ErrorCode 枚举）
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, null);
    }

    /**
     * 错误响应（基于 ErrorCode 枚举 + 路径）
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String path) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errorCode(errorCode.getCode())
                .requestId(generateRequestId())
                .timestamp(System.currentTimeMillis())
                .path(path)
                .build();
    }

    /**
     * 错误响应（基于 ErrorCode 枚举 + 自定义消息）
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage, String path) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(customMessage)
                .errorCode(errorCode.getCode())
                .requestId(generateRequestId())
                .timestamp(System.currentTimeMillis())
                .path(path)
                .build();
    }

    // ========== 错误响应（兼容旧接口，直接传入 code + message） ==========

    /**
     * 错误响应（自定义 code + message）
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return error(code, message, null);
    }

    /**
     * 错误响应（自定义 code + message + path）
     */
    public static <T> ApiResponse<T> error(Integer code, String message, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .errorCode(code)
                .requestId(generateRequestId())
                .timestamp(System.currentTimeMillis())
                .path(path)
                .build();
    }

    // ========== 内部工具方法 ==========

    /**
     * 生成请求追踪ID
     */
    private static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
