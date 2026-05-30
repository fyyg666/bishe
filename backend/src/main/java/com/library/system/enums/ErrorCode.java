package com.library.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一错误码枚举 
 * 按模块分段编码，便于快速定位问题来源
 *
 * 编码规则：
 *   1xxx - 认证模块
 *   2xxx - 通用业务模块
 *   3xxx - 图书模块
 *   4xxx - 借阅模块
 *   5xxx - 读者模块
 *   6xxx - 座位预约模块
 *   7xxx - 公告模块
 *   8xxx - 志愿服务模块
 *   9xxx - 信用积分模块
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 通用模块 2000-2999 ==========
    SUCCESS(2000, "操作成功", HttpStatus.OK),
    UNKNOWN_ERROR(2999, "未知错误", HttpStatus.INTERNAL_SERVER_ERROR),
    RESOURCE_NOT_FOUND(2001, "资源不存在", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(2002, "资源已存在", HttpStatus.CONFLICT),
    INSUFFICIENT_PERMISSION(2003, "权限不足", HttpStatus.FORBIDDEN),
    PARAMETER_ERROR(2004, "参数校验失败", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(2005, "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    RATE_LIMIT_EXCEEDED(2006, "请求频率超限", HttpStatus.TOO_MANY_REQUESTS),
    CONCURRENT_OPERATION(2007, "并发操作冲突，请重试", HttpStatus.CONFLICT), 

    // ========== 认证模块 1000-1999 ==========
    AUTH_FAILED(1001, "认证失败", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1002, "令牌已过期", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1003, "令牌无效", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1004, "账户已锁定", HttpStatus.FORBIDDEN),
    USERNAME_EXISTS(1005, "用户名已存在", HttpStatus.CONFLICT),
    PASSWORD_COMPLEXITY(1006, "密码复杂度不足", HttpStatus.BAD_REQUEST),
    ACCOUNT_DISABLED(1007, "账户已禁用", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_INVALID(1008, "刷新令牌无效", HttpStatus.UNAUTHORIZED),
    TOO_MANY_LOGIN_ATTEMPTS(1009, "登录尝试次数过多", HttpStatus.UNAUTHORIZED),

    // ========== 图书模块 3000-3999 ==========
    BOOK_NOT_FOUND(3001, "图书不存在", HttpStatus.NOT_FOUND),
    BOOK_NOT_AVAILABLE(3002, "图书不可借", HttpStatus.BAD_REQUEST),
    ISBN_DUPLICATE(3003, "ISBN已存在", HttpStatus.CONFLICT),
    BOOK_CATEGORY_ERROR(3004, "图书分类错误", HttpStatus.BAD_REQUEST),
    BOOK_STOCK_ERROR(3005, "库存不足", HttpStatus.BAD_REQUEST),

    // ========== 借阅模块 4000-4999 ==========
    BORROW_LIMIT_EXCEEDED(4001, "借阅数量超限", HttpStatus.BAD_REQUEST),
    BORROW_RECORD_NOT_FOUND(4002, "借阅记录不存在", HttpStatus.NOT_FOUND),
    BORROW_ALREADY_RETURNED(4003, "图书已归还", HttpStatus.BAD_REQUEST),
    BORROW_OVERDUE(4004, "借阅已逾期", HttpStatus.BAD_REQUEST),
    RETURN_FAILED(4005, "归还失败", HttpStatus.BAD_REQUEST),
    RENEW_LIMIT_EXCEEDED(4006, "续借次数超限", HttpStatus.BAD_REQUEST),

    // ========== 读者模块 5000-5999 ==========
    READER_NOT_FOUND(5001, "读者不存在", HttpStatus.NOT_FOUND),
    READER_ALREADY_EXISTS(5002, "读者已存在", HttpStatus.CONFLICT),
    READER_STATUS_ERROR(5003, "读者状态异常", HttpStatus.BAD_REQUEST),

    // ========== 座位预约模块 6000-6999 ==========
    SEAT_NOT_FOUND(6001, "座位不存在", HttpStatus.NOT_FOUND),
    SEAT_NOT_AVAILABLE(6002, "座位不可预约", HttpStatus.BAD_REQUEST),
    SEAT_RESERVATION_CONFLICT(6003, "座位预约时间冲突", HttpStatus.CONFLICT),
    SEAT_RESERVATION_NOT_FOUND(6004, "预约记录不存在", HttpStatus.NOT_FOUND),
    SEAT_CHECK_IN_EXPIRED(6005, "签到已超时", HttpStatus.BAD_REQUEST),
    SEAT_CANCEL_TOO_LATE(6006, "取消预约时间过晚", HttpStatus.BAD_REQUEST),

    // ========== 公告模块 7000-7999 ==========
    ANNOUNCEMENT_NOT_FOUND(7001, "公告不存在", HttpStatus.NOT_FOUND),
    ANNOUNCEMENT_EXPIRED(7002, "公告已过期", HttpStatus.BAD_REQUEST),

    // ========== 志愿服务模块 8000-8999 ==========
    VOLUNTEER_NOT_FOUND(8001, "志愿记录不存在", HttpStatus.NOT_FOUND),
    VOLUNTEER_HOURS_EXCEEDED(8002, "服务时长超限", HttpStatus.BAD_REQUEST),
    VOLUNTEER_STATUS_ERROR(8003, "志愿服务状态异常", HttpStatus.BAD_REQUEST),

    // ========== 信用积分模块 9000-9999 ==========
    CREDIT_SCORE_LOW(9001, "信用积分不足", HttpStatus.BAD_REQUEST),
    CREDIT_LOG_NOT_FOUND(9002, "信用记录不存在", HttpStatus.NOT_FOUND),
    CREDIT_ADJUST_FAILED(9003, "积分调整失败", HttpStatus.INTERNAL_SERVER_ERROR);

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误描述
     */
    private final String message;

    /**
     * 关联的HTTP状态码 
     */
    private final HttpStatus httpStatus;
}
