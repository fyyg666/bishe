package com.library.system.common;

/**
 * 系统常量定义
 *
 * @author Library Team
 * @version 2.0.0
 */
public class Constants {

    /** JWT Token相关常量 */
    public static class Token {
        /** Access Token过期时间：2小时 */
        public static final long ACCESS_TOKEN_EXPIRATION = 2 * 60 * 60 * 1000;
        
        /** Refresh Token过期时间：7天 */
        public static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000;
        
        /** Token前缀 */
        public static final String TOKEN_PREFIX = "token:";
        
        /** 黑名单前缀 */
        public static final String BLACKLIST_PREFIX = "blacklist:";
        
        /** Authorization头前缀 */ 
        public static final String BEARER_PREFIX = "Bearer ";
        
        /** Spring Security角色前缀 */ 
        public static final String ROLE_PREFIX = "ROLE_";
        
        /** Access Token类型标识 */ 
        public static final String TOKEN_TYPE_ACCESS = "ACCESS";
        
        /** Refresh Token类型标识 */ 
        public static final String TOKEN_TYPE_REFRESH = "REFRESH";
        
        /** 匿名用户标识 */ 
        public static final String ANONYMOUS_USER = "anonymousUser";
    }

    /** 用户角色常量 */
    public static class Role {
        /** 管理员 */
        public static final String ADMIN = "ADMIN";
        
        /** 图书管理员 */
        public static final String LIBRARIAN = "LIBRARIAN";
        
        /** 普通读者 */
        public static final String READER = "READER";
        
        /** 志愿者 */
        public static final String VOLUNTEER = "VOLUNTEER";
    }

    /** 用户状态常量 */
    public static class UserStatus {
        /** 正常 */
        public static final String NORMAL = "NORMAL";
        
        /** 禁用 */
        public static final String DISABLED = "DISABLED";
        
        /** 锁定 */
        public static final String LOCKED = "LOCKED";
    }

    /** 图书状态常量 */
    public static class BookStatus {
        /** 上架（可借阅） */
        public static final Integer NORMAL = 0;
        
        /** 下架（不可借阅） */
        public static final Integer OFFLINE = 1;
        
        // 语义别名，方便理解
        /** 上架（可借阅）— NORMAL 的语义别名 */
        public static final Integer ON_SHELF = 0;
        
        /** 下架（不可借阅） */
        public static final Integer OFF_SHELF = 1;
    }

    /** 借阅状态常量 */
    public static class BorrowStatus {
        /** 借阅中 */
        public static final String BORROWING = "BORROWING";
        
        /** 已归还 */
        public static final String RETURNED = "RETURNED";
        
        /** 逾期 */
        public static final String OVERDUE = "OVERDUE";
        
        /** 已预约 */
        public static final String RESERVED = "RESERVED";
    }

    /** 座位预约状态常量 */
    public static class SeatStatus {
        /** 可用 */
        public static final String AVAILABLE = "AVAILABLE";
        
        /** 已预约 */
        public static final String RESERVED = "RESERVED";
        
        /** 使用中 */
        public static final String OCCUPIED = "OCCUPIED";
        
        /** 维护中 */
        public static final String MAINTENANCE = "MAINTENANCE";
    }

    /** 预约状态常量 */
    public static class ReservationStatus {
        /** 待签到 */
        public static final String PENDING = "PENDING";
        
        /** 已签到 */
        public static final String CHECKED_IN = "CHECKED_IN";
        
        /** 已完成 */
        public static final String COMPLETED = "COMPLETED";
        
        /** 已取消 */
        public static final String CANCELLED = "CANCELLED";
        
        /** 违约 */
        public static final String VIOLATED = "VIOLATED";
    }

    /** 信用积分常量 */
    public static class Credit {
        /** 初始积分 */
        public static final int INITIAL_SCORE = 100;
        
        /** 最高积分 */
        public static final int MAX_SCORE = 300;
        
        /** 最低积分 */
        public static final int MIN_SCORE = 0;
        
        /** 借阅奖励积分 */
        public static final int BORROW_REWARD = 5;
        
        /** 按时归还奖励积分 */
        public static final int RETURN_ON_TIME = 1;
        
        /** 提前归还奖励积分 */
        public static final int RETURN_EARLY = 2;
        
        /** 逾期每天扣分 */
        public static final int OVERDUE_PER_DAY = 5;
        
        /** 座位未签到扣分（常量名保持不变，值由业务逻辑使用） */
        public static final int NO_SHOW = 2;
        
        /** 图书损坏扣分 */
        public static final int DAMAGE_PENALTY = 50;
        
        /** 图书丢失扣分 */
        public static final int LOST_PENALTY = 100;
        
        /** 志愿服务每小时积分 */
        public static final int VOLUNTEER_PER_HOUR = 10;
        
        /** 志愿服务12小时加1分（旧规则保留用于兼容） */
        public static final int VOLUNTEER_HOURS_PER_CREDIT = 12;
        
        /** 签到奖励积分 */
        public static final int CHECKIN_REWARD = 1;
        
        /** 铜牌等级下限 */
        public static final int BRONZE_THRESHOLD = 60;
        
        /** 银牌等级下限 */
        public static final int SILVER_THRESHOLD = 120;
        
        /** 金牌等级下限 */
        public static final int GOLD_THRESHOLD = 180;
        
        /** 白金等级下限 */
        public static final int PLATINUM_THRESHOLD = 240;
    }

    /** Redis缓存常量 */
    public static class Cache {
        /** 用户缓存 */
        public static final String USER = "user:";
        
        /** 图书缓存 */
        public static final String BOOK = "book:";
        
        /** 热门图书缓存 */
        public static final String HOT_BOOKS = "hot_books";
        
        /** 分类缓存 */
        public static final String CATEGORY = "category:";
        
        /** 布隆过滤器前缀 */
        public static final String BLOOM_FILTER = "bloom:";
    }

    /** 分页常量 */
    public static class Page {
        /** 默认页码 */
        public static final int DEFAULT_PAGE = 1;
        
        /** 默认每页数量 */
        public static final int DEFAULT_SIZE = 20;
        
        /** 最大每页数量 */
        public static final int MAX_SIZE = 100;
    }

    /** 借阅限制常量 */
    public static class BorrowLimit {
        /** 最大借阅数量 */
        public static final int MAX_BORROW_COUNT = 5;
        
        /** 借阅天数 */
        public static final int BORROW_DAYS = 30;
        
        /** 续借次数上限 */
        public static final int MAX_RENEW_TIMES = 1;
        
        /** 续借天数 */
        public static final int RENEW_DAYS = 15;
    }

    /** 安全相关常量 */ 
    public static class Security {
        /** 默认重置密码 */
        public static final String DEFAULT_PASSWORD = "123456";

        /** 会话超时时间（分钟） - FIXED: SEC-P3-04 */
        public static final int SESSION_TIMEOUT_MINUTES = 30;

        /** 登录失败最大尝试次数 - FIXED: SEC-P3-04 */
        public static final int MAX_LOGIN_ATTEMPTS = 5;

        /** 账户锁定时长（分钟） - FIXED: SEC-P3-04 */
        public static final int ACCOUNT_LOCK_DURATION_MINUTES = 15;
    }

    /** 座位预约限制常量 */
    public static class SeatLimit {
        /** 预约有效期（小时） */
        public static final int RESERVATION_VALID_HOURS = 24;
        
        /** 最长使用时长（小时） */
        public static final int MAX_USAGE_HOURS = 8;
        
        /** 每日最大预约次数 */
        public static final int DAILY_MAX_RESERVATIONS = 2;
        
        /** 违约次数阈值 */
        public static final int VIOLATION_THRESHOLD = 3;
    }

    /** 分布式锁消息常量 */
    public static class LockMessage {
        /** 获取锁失败 */
        public static final String ACQUIRE_FAILED = "系统繁忙，请稍后重试";

        /** 操作被中断 */
        public static final String OPERATION_INTERRUPTED = "操作被中断";

        /** 业务逻辑执行异常 */
        public static final String EXECUTION_ERROR = "业务逻辑执行异常：";
    }
}
