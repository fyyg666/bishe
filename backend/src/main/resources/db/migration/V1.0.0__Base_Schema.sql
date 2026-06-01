-- ============================================================
-- 图书馆管理系统 V1.0.0 基础数据库结构
-- 根据所有实体类生成完整建表语句
-- 数据库: library_system
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 系统用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'READER' COMMENT '角色: ADMIN/LIBRARIAN/READER/VOLUNTEER',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL/DISABLED/LOCKED',
    `credit_score` INT DEFAULT 0 COMMENT '积分',
    `card_number` VARCHAR(50) DEFAULT NULL COMMENT '读者卡号',
    `borrow_count` INT DEFAULT 0 COMMENT '借阅数量',
    `violation_count` INT DEFAULT 0 COMMENT '违约次数',
    `ban_until` DATETIME DEFAULT NULL COMMENT '封禁到期时间',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_card_number` (`card_number`),
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`),
    INDEX `idx_credit_score` (`credit_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN', 'NORMAL'),
(2, 'librarian', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '图书管理员', 'LIBRARIAN', 'NORMAL');

-- ============================================================
-- 2. 图书分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS `book_category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `code` VARCHAR(50) NOT NULL COMMENT '分类编码',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `create_time` VARCHAR(50) DEFAULT NULL COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_code` (`code`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书分类表';

INSERT IGNORE INTO `book_category` (`id`, `name`, `code`, `parent_id`, `sort_order`) VALUES
(1, '计算机科学', 'CS', NULL, 1),
(2, '文学', 'LIT', NULL, 2),
(3, '历史', 'HIS', NULL, 3),
(4, '哲学', 'PHI', NULL, 4),
(5, '自然科学', 'SCI', NULL, 5),
(6, '社会科学', 'SOC', NULL, 6),
(7, '艺术', 'ART', NULL, 7),
(8, '语言', 'LANG', NULL, 8);

-- ============================================================
-- 3. 图书表
-- ============================================================
CREATE TABLE IF NOT EXISTS `book` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图书ID',
    `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID',
    `isbn` VARCHAR(20) NOT NULL COMMENT 'ISBN编号',
    `title` VARCHAR(200) NOT NULL COMMENT '书名',
    `author` VARCHAR(100) DEFAULT NULL COMMENT '作者',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `publisher` VARCHAR(100) DEFAULT NULL COMMENT '出版社',
    `publish_date` DATE DEFAULT NULL COMMENT '出版日期',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格',
    `total_quantity` INT DEFAULT 0 COMMENT '总数量',
    `stock` INT DEFAULT 0 COMMENT '可借数量',
    `borrow_count` INT DEFAULT 0 COMMENT '借阅次数',
    `status` INT DEFAULT 0 COMMENT '状态: 0-上架(可借), 1-下架(不可借)',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_isbn` (`isbn`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书表';

-- ============================================================
-- 4. 借阅记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `borrow_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '借阅记录ID',
    `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `borrow_date` DATETIME DEFAULT NULL COMMENT '借阅日期',
    `due_date` DATETIME DEFAULT NULL COMMENT '应归还日期',
    `return_date` DATETIME DEFAULT NULL COMMENT '实际归还日期',
    `status` VARCHAR(20) NOT NULL DEFAULT 'BORROWING' COMMENT '状态: BORROWING/RETURNED/OVERDUE/RESERVED',
    `renew_count` INT DEFAULT 0 COMMENT '续借次数',
    `fine_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '罚款金额',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_borrow_date` (`borrow_date`),
    INDEX `idx_due_date` (`due_date`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_user_due` (`user_id`, `due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅记录表';

-- ============================================================
-- 5. 借阅规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS `borrow_rule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规则ID',
    `reader_type` VARCHAR(20) NOT NULL COMMENT '读者类型: READER/LIBRARIAN/ADMIN',
    `book_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '图书类型: NORMAL/REFERENCE',
    `max_borrow` INT NOT NULL DEFAULT 5 COMMENT '最大借阅数量',
    `max_days` INT NOT NULL DEFAULT 30 COMMENT '最大借阅天数',
    `max_renew` INT NOT NULL DEFAULT 1 COMMENT '最大续借次数',
    `renew_days` INT NOT NULL DEFAULT 15 COMMENT '每次续借天数',
    `fine_per_day` DECIMAL(10,2) NOT NULL DEFAULT 0.10 COMMENT '每日罚款金额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE INDEX `idx_reader_book_type` (`reader_type`, `book_type`),
    INDEX `idx_reader_type` (`reader_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅规则表';

INSERT IGNORE INTO `borrow_rule` (`reader_type`, `book_type`, `max_borrow`, `max_days`, `max_renew`, `renew_days`, `fine_per_day`) VALUES
('READER', 'NORMAL', 5, 30, 1, 15, 0.10),
('READER', 'REFERENCE', 2, 14, 0, 0, 0.10),
('LIBRARIAN', 'NORMAL', 10, 60, 2, 15, 0.10),
('LIBRARIAN', 'REFERENCE', 5, 30, 1, 15, 0.10),
('ADMIN', 'NORMAL', 10, 60, 2, 15, 0.10),
('ADMIN', 'REFERENCE', 5, 30, 1, 15, 0.10);

-- ============================================================
-- 6. 图书预约排队表
-- ============================================================
CREATE TABLE IF NOT EXISTS `book_reservation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '预约用户ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/NOTIFIED/CANCELLED/FULFILLED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `notify_time` DATETIME DEFAULT NULL COMMENT '通知时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_book_status` (`book_id`, `status`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_notify_time` (`notify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书预约排队表';

-- ============================================================
-- 7. 座位表
-- ============================================================
CREATE TABLE IF NOT EXISTS `seat` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '座位ID',
    `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID',
    `room_id` BIGINT DEFAULT NULL COMMENT '阅览室ID',
    `seat_number` VARCHAR(20) NOT NULL COMMENT '座位编号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE/MAINTENANCE',
    `created_at` VARCHAR(50) DEFAULT NULL COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_seat_number` (`seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位表';

-- ============================================================
-- 8. 座位预约表
-- ============================================================
CREATE TABLE IF NOT EXISTS `seat_reservation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `room_id` BIGINT DEFAULT NULL COMMENT '阅览室ID',
    `seat_id` BIGINT NOT NULL COMMENT '座位ID',
    `reservation_date` DATETIME DEFAULT NULL COMMENT '预约日期',
    `start_time` DATETIME DEFAULT NULL COMMENT '预约开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '预约结束时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/CHECKED_IN/COMPLETED/CANCELLED/NO_SHOW',
    `check_in_time` DATETIME DEFAULT NULL COMMENT '签到时间',
    `check_out_time` DATETIME DEFAULT NULL COMMENT '签退时间',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_seat_id` (`seat_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_reservation_date` (`reservation_date`),
    INDEX `idx_user_date_status` (`user_id`, `reservation_date`, `status`),
    INDEX `idx_seat_date` (`seat_id`, `reservation_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位预约表';

-- ============================================================
-- 9. 公告表
-- ============================================================
CREATE TABLE IF NOT EXISTS `announcement` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT DEFAULT NULL COMMENT '内容',
    `type` VARCHAR(20) NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型: NOTICE/ACTIVITY/URGENT',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ARCHIVED',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `is_top` INT DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `created_by` BIGINT DEFAULT NULL COMMENT '发布人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_status` (`status`),
    INDEX `idx_type` (`type`),
    INDEX `idx_is_top` (`is_top`),
    INDEX `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- ============================================================
-- 10. 通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT DEFAULT NULL COMMENT '通知内容',
    `type` VARCHAR(50) DEFAULT 'SYSTEM' COMMENT '通知类型: SYSTEM/BORROW/SEAT/CREDIT',
    `status` VARCHAR(20) DEFAULT 'UNREAD' COMMENT '通知状态: UNREAD/READ',
    `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `biz_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `announcement_id` BIGINT DEFAULT NULL COMMENT '关联公告ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_user_type` (`user_id`, `type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';

-- ============================================================
-- 11. 系统操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `module` VARCHAR(100) DEFAULT NULL COMMENT '操作模块',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作类型',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `result` TEXT DEFAULT NULL COMMENT '返回结果',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '操作地点',
    `duration` BIGINT DEFAULT NULL COMMENT '耗时(毫秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_oplog_user_created` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- ============================================================
-- 12. 系统配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_config` (
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `description`) VALUES
('credit.borrow_reward', '5', '借阅奖励积分'),
('credit.return_on_time', '1', '按时归还奖励积分'),
('credit.return_early', '2', '提前归还奖励积分'),
('credit.overdue_per_day', '5', '逾期每天扣除积分'),
('credit.no_show', '2', '预约未签到扣除积分'),
('credit.damage_penalty', '50', '图书损坏扣除积分'),
('credit.lost_penalty', '100', '图书丢失扣除积分'),
('credit.volunteer_per_hour', '10', '每小时志愿服务奖励积分'),
('credit.checkin_reward', '1', '座位签到奖励积分'),
('seat.cancel_before_hours', '2', '座位预约取消提前小时数'),
('library.default_password', '123456', '读者默认密码'),
('library.default_max_borrow', '5', '默认最大借阅数量');

-- ============================================================
-- 13. 积分日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `credit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `credit_change` INT NOT NULL COMMENT '变动值（正数为增加，负数为减少）',
    `credit_balance` INT NOT NULL COMMENT '变动后积分',
    `change_type` VARCHAR(50) DEFAULT NULL COMMENT '变动类型',
    `biz_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_change_type` (`change_type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分日志表';

-- ============================================================
-- 14. 赔偿订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `compensation_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '赔偿订单ID',
    `order_no` VARCHAR(50) DEFAULT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `borrow_id` BIGINT DEFAULT NULL COMMENT '借阅记录ID',
    `book_id` BIGINT DEFAULT NULL COMMENT '图书ID',
    `book_title` VARCHAR(200) DEFAULT NULL COMMENT '图书名称',
    `isbn` VARCHAR(20) DEFAULT NULL COMMENT 'ISBN',
    `comp_type` VARCHAR(20) NOT NULL DEFAULT 'LOST' COMMENT '赔偿类型: LOST/DAMAGED',
    `amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '赔偿金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PAID/CANCELLED',
    `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
    `credit_deducted` INT DEFAULT 0 COMMENT '已扣除积分',
    `volunteer_hours` DECIMAL(5,2) DEFAULT 0.00 COMMENT '已抵扣志愿时长',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_order_no` (`order_no`),
    INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赔偿订单表';

-- ============================================================
-- 15. 志愿服务表
-- ============================================================
CREATE TABLE IF NOT EXISTS `volunteer_service` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `service_date` DATETIME DEFAULT NULL COMMENT '服务日期',
    `start_time` DATETIME DEFAULT NULL COMMENT '服务开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '服务结束时间',
    `service_hours` DECIMAL(5,2) DEFAULT 0.00 COMMENT '服务时长（小时）',
    `service_type` VARCHAR(50) DEFAULT NULL COMMENT '服务类型',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '服务内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_service_date` (`service_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿服务表';

-- ============================================================
-- 16. 阅览室表
-- ============================================================
CREATE TABLE IF NOT EXISTS `reading_room` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '阅览室ID',
    `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID',
    `name` VARCHAR(100) NOT NULL COMMENT '阅览室名称',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '位置描述',
    `total_seats` INT DEFAULT 0 COMMENT '座位容量',
    `open_time` VARCHAR(20) DEFAULT NULL COMMENT '开放时间',
    `close_time` VARCHAR(20) DEFAULT NULL COMMENT '关闭时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态: OPEN/CLOSED/MAINTENANCE',
    `create_time` VARCHAR(50) DEFAULT NULL COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅览室表';

INSERT IGNORE INTO `reading_room` (`id`, `name`, `location`, `total_seats`, `open_time`, `close_time`, `status`) VALUES
(1, '自习室A', '一楼东侧', 60, '08:00', '22:00', 'OPEN'),
(2, '自习室B', '二楼西侧', 40, '08:00', '22:00', 'OPEN'),
(3, '电子阅览室', '三楼南侧', 30, '09:00', '21:00', 'OPEN');

-- ============================================================
-- 17. 分馆表
-- ============================================================
CREATE TABLE IF NOT EXISTS `branch` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分馆ID',
    `name` VARCHAR(100) NOT NULL COMMENT '分馆名称',
    `code` VARCHAR(50) NOT NULL COMMENT '分馆编码',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `opening_hours` VARCHAR(255) DEFAULT NULL COMMENT '开放时间描述',
    `status` INT DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上级分馆ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_code` (`code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分馆表';

INSERT IGNORE INTO `branch` (`id`, `name`, `code`, `address`, `status`) VALUES
(1, '总馆', 'MAIN', '图书馆总馆', 1),
(2, '东区分馆', 'EAST', '东区分馆', 1),
(3, '西区分馆', 'WEST', '西区分馆', 1);

-- ============================================================
-- 18. 供应商表
-- ============================================================
CREATE TABLE IF NOT EXISTS `vendor` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '供应商ID',
    `name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
    `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

-- ============================================================
-- 19. 预算基金表
-- ============================================================
CREATE TABLE IF NOT EXISTS `budget_fund` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预算ID',
    `name` VARCHAR(100) NOT NULL COMMENT '预算名称',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '总额',
    `used_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '已用金额',
    `fiscal_year` INT NOT NULL COMMENT '财年',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_fiscal_year` (`fiscal_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算基金表';

-- ============================================================
-- 20. 荐购表
-- ============================================================
CREATE TABLE IF NOT EXISTS `purchase_suggestion` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '荐购ID',
    `user_id` BIGINT NOT NULL COMMENT '荐购用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '书名',
    `author` VARCHAR(100) DEFAULT NULL COMMENT '作者',
    `isbn` VARCHAR(20) DEFAULT NULL COMMENT 'ISBN',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '荐购理由',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荐购表';

-- ============================================================
-- 21. 采购订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `purchase_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '采购单ID',
    `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID',
    `vendor_id` BIGINT DEFAULT NULL COMMENT '供应商ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '采购单号',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '采购单标题',
    `supplier` VARCHAR(100) DEFAULT NULL COMMENT '供应商',
    `total_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '总金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/RECEIVED/CANCELLED',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_order_no` (`order_no`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单表';

-- ============================================================
-- 22. 采购订单明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS `purchase_order_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '采购明细ID',
    `order_id` BIGINT NOT NULL COMMENT '采购单ID',
    `book_title` VARCHAR(200) NOT NULL COMMENT '书名',
    `isbn` VARCHAR(20) DEFAULT NULL COMMENT 'ISBN',
    `author` VARCHAR(100) DEFAULT NULL COMMENT '作者',
    `publisher` VARCHAR(100) DEFAULT NULL COMMENT '出版社',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '采购数量',
    `received_quantity` INT DEFAULT 0 COMMENT '已收货数量',
    `cataloged_quantity` INT DEFAULT 0 COMMENT '已入库数量',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RECEIVED/CATALOGED',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_isbn` (`isbn`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';

-- ============================================================
-- 23. 数字资源表
-- ============================================================
CREATE TABLE IF NOT EXISTS `digital_resource` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '数字资源ID',
    `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID',
    `title` VARCHAR(200) NOT NULL COMMENT '题名',
    `author` VARCHAR(100) DEFAULT NULL COMMENT '作者',
    `isbn` VARCHAR(20) DEFAULT NULL COMMENT 'ISBN',
    `resource_type` VARCHAR(20) NOT NULL DEFAULT 'EBOOK' COMMENT '类型: EBOOK/AUDIO/VIDEO/DATABASE',
    `format` VARCHAR(20) NOT NULL DEFAULT 'PDF' COMMENT '格式: PDF/EPUB/MP3/MP4',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    `access_url` VARCHAR(500) DEFAULT NULL COMMENT '访问地址',
    `provider` VARCHAR(100) DEFAULT NULL COMMENT '提供商',
    `access_mode` VARCHAR(20) NOT NULL DEFAULT 'ONLINE' COMMENT '访问方式: ONLINE/DOWNLOAD/BOTH',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `description` TEXT DEFAULT NULL COMMENT '简介',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    `status` INT DEFAULT 0 COMMENT '状态: 0-可用, 1-下架',
    `borrow_count` INT DEFAULT 0 COMMENT '访问次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_resource_type` (`resource_type`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字资源表';

-- ============================================================
-- 24. MARC21记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `marc_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `record_type` VARCHAR(10) NOT NULL DEFAULT 'BIB' COMMENT '记录类型: BIB-书目/AUTH-规范',
    `leader` VARCHAR(24) DEFAULT NULL COMMENT 'MARC21头标区',
    `control_number` VARCHAR(50) DEFAULT NULL COMMENT '控制号',
    `book_id` BIGINT DEFAULT NULL COMMENT '关联图书ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/APPROVED',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_record_type` (`record_type`),
    INDEX `idx_control_number` (`control_number`),
    INDEX `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC21记录表';

-- ============================================================
-- 25. MARC21字段表
-- ============================================================
CREATE TABLE IF NOT EXISTS `marc_field` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '字段ID',
    `record_id` BIGINT NOT NULL COMMENT 'MARC记录ID',
    `tag` VARCHAR(3) NOT NULL COMMENT '字段标签',
    `indicator1` VARCHAR(1) DEFAULT ' ' COMMENT '指示符1',
    `indicator2` VARCHAR(1) DEFAULT ' ' COMMENT '指示符2',
    `subfields` JSON DEFAULT NULL COMMENT '子字段JSON',
    `display_value` TEXT DEFAULT NULL COMMENT '显示值',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_tag` (`tag`),
    INDEX `idx_record_tag` (`record_id`, `tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC21字段表';

-- ============================================================
-- 26. MARC编目框架表
-- ============================================================
CREATE TABLE IF NOT EXISTS `marc_framework` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '框架ID',
    `name` VARCHAR(100) NOT NULL COMMENT '框架名称',
    `code` VARCHAR(20) NOT NULL COMMENT '框架代码',
    `record_type` VARCHAR(10) NOT NULL DEFAULT 'BIB' COMMENT '适用记录类型',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认框架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC编目框架表';

INSERT IGNORE INTO `marc_framework` (`name`, `code`, `record_type`, `description`, `is_default`) VALUES
('图书', 'BOOK', 'BIB', '普通图书编目框架', 1),
('连续出版物', 'SERIAL', 'BIB', '期刊/报纸编目框架', 0),
('地图', 'MAP', 'BIB', '地图资料编目框架', 0),
('音像', 'AV', 'BIB', '音像资料编目框架', 0);

-- ============================================================
-- 27. MARC框架字段配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `marc_framework_field` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '字段配置ID',
    `framework_id` BIGINT NOT NULL COMMENT '框架ID',
    `tag` VARCHAR(3) NOT NULL COMMENT '字段标签',
    `indicator1` VARCHAR(1) DEFAULT ' ' COMMENT '默认指示符1',
    `indicator2` VARCHAR(1) DEFAULT ' ' COMMENT '默认指示符2',
    `required` TINYINT DEFAULT 0 COMMENT '是否必填',
    `repeatable` TINYINT DEFAULT 1 COMMENT '是否可重复',
    `default_subfields` JSON DEFAULT NULL COMMENT '默认子字段',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    INDEX `idx_framework_id` (`framework_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC框架字段配置表';

INSERT IGNORE INTO `marc_framework_field` (`framework_id`, `tag`, `indicator1`, `indicator2`, `required`, `repeatable`, `default_subfields`, `sort_order`) VALUES
(1, '001', ' ', ' ', 1, 0, NULL, 1),
(1, '010', ' ', ' ', 1, 0, '[{"code":"a","value":""},{"code":"d","value":""}]', 2),
(1, '100', '1', ' ', 1, 0, '[{"code":"a","value":""}]', 3),
(1, '245', '1', '0', 1, 0, '[{"code":"a","value":""},{"code":"b","value":""},{"code":"c","value":""}]', 4),
(1, '250', ' ', ' ', 0, 0, '[{"code":"a","value":""}]', 5),
(1, '260', ' ', ' ', 1, 0, '[{"code":"a","value":""},{"code":"b","value":""},{"code":"c","value":""}]', 6),
(1, '300', ' ', ' ', 0, 0, '[{"code":"a","value":""},{"code":"b","value":""},{"code":"c","value":""}]', 7),
(1, '650', ' ', '0', 0, 1, '[{"code":"a","value":""}]', 8),
(1, '905', ' ', ' ', 0, 0, '[{"code":"a","value":""}]', 9);

-- ============================================================
-- 28. 规范记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `authority_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规范记录ID',
    `authority_type` VARCHAR(20) NOT NULL COMMENT '类型: PERSONAL/CORPORATE/TOPIC',
    `heading` VARCHAR(255) NOT NULL COMMENT '规范标目',
    `variant_headings` JSON DEFAULT NULL COMMENT '变异形式列表',
    `source` VARCHAR(50) DEFAULT NULL COMMENT '来源',
    `source_id` VARCHAR(50) DEFAULT NULL COMMENT '来源ID',
    `note` TEXT DEFAULT NULL COMMENT '附注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_type_heading` (`authority_type`, `heading`),
    INDEX `idx_source` (`source`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规范记录表';

-- ============================================================
-- 29. Z39.50数据源配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `z3950_source` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '数据源ID',
    `name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
    `host` VARCHAR(255) NOT NULL COMMENT '主机地址',
    `port` INT NOT NULL DEFAULT 210 COMMENT '端口',
    `database` VARCHAR(100) NOT NULL COMMENT '数据库名',
    `protocol` VARCHAR(10) NOT NULL DEFAULT 'SRU' COMMENT '协议: SRU/Z3950',
    `charset` VARCHAR(20) DEFAULT 'UTF-8' COMMENT '字符集',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `timeout` INT DEFAULT 30 COMMENT '超时(秒)',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Z39.50数据源配置表';

INSERT IGNORE INTO `z3950_source` (`name`, `host`, `port`, `database`, `protocol`, `charset`, `is_enabled`, `timeout`) VALUES
('Library of Congress', 'z3950.loc.gov', 210, 'Voyager', 'SRU', 'UTF-8', 1, 30),
('中国国家图书馆', 'opac.nlc.cn', 210, 'F', 'SRU', 'UTF-8', 1, 30),
('CALIS高校图书馆', 'z3950.calis.edu.cn', 210, 'CALIS', 'SRU', 'UTF-8', 0, 30);

-- ============================================================
-- 30. 日统计表
-- ============================================================
CREATE TABLE IF NOT EXISTS `statistics_daily` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_borrows` INT DEFAULT 0 COMMENT '总借阅数',
    `total_returns` INT DEFAULT 0 COMMENT '总归还数',
    `total_overdue` INT DEFAULT 0 COMMENT '总逾期数',
    `total_reservations` INT DEFAULT 0 COMMENT '总预约数',
    `total_checkins` INT DEFAULT 0 COMMENT '总签到数',
    `total_new_users` INT DEFAULT 0 COMMENT '新增用户数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日统计表';

-- ============================================================
-- 31. 报表模板表
-- ============================================================
CREATE TABLE IF NOT EXISTS `report_template` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    `name` VARCHAR(100) NOT NULL COMMENT '报表名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `sql_template` TEXT NOT NULL COMMENT 'SQL模板',
    `parameters` JSON DEFAULT NULL COMMENT '参数定义',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义报表模板表';

INSERT IGNORE INTO `report_template` (`name`, `description`, `sql_template`, `parameters`, `category`, `created_by`) VALUES
('月度借阅统计', '按月统计借阅数量',
 'SELECT DATE_FORMAT(borrow_date, ''%Y-%m'') as month, COUNT(*) as borrow_count FROM borrow_record WHERE deleted=0 AND borrow_date BETWEEN :startDate AND :endDate GROUP BY month ORDER BY month',
 '[{"name":"startDate","label":"开始日期","type":"date"},{"name":"endDate","label":"结束日期","type":"date"}]',
 '借阅统计', 1),
('分类图书统计', '按分类统计图书数量和借阅次数',
 'SELECT bc.name as category, COUNT(b.id) as book_count, SUM(b.borrow_count) as total_borrows FROM book b LEFT JOIN book_category bc ON b.category_id = bc.id WHERE b.deleted=0 GROUP BY bc.name ORDER BY book_count DESC',
 '[]',
 '图书统计', 1),
('读者借阅排行', '读者借阅数量排行榜',
 'SELECT u.username, u.real_name, u.borrow_count FROM sys_user u WHERE u.deleted=0 AND u.borrow_count > 0 ORDER BY u.borrow_count DESC LIMIT :topN',
 '[{"name":"topN","label":"排行人数","type":"number","default":20}]',
 '读者统计', 1);

-- ============================================================
-- 32. 期刊订阅表
-- ============================================================
CREATE TABLE IF NOT EXISTS `serial_subscription` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订阅ID',
    `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID',
    `title` VARCHAR(200) NOT NULL COMMENT '刊名',
    `issn` VARCHAR(20) DEFAULT NULL COMMENT 'ISSN',
    `vendor_id` BIGINT DEFAULT NULL COMMENT '供应商ID',
    `fund_id` BIGINT DEFAULT NULL COMMENT '预算ID',
    `start_date` DATE DEFAULT NULL COMMENT '订阅开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '订阅结束日期',
    `frequency` VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' COMMENT '频率: DAILY/WEEKLY/BIWEEKLY/MONTHLY/QUARTERLY/YEARLY',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/TERMINATED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_issn` (`issn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊订阅表';

-- ============================================================
-- 33. 期刊到刊表
-- ============================================================
CREATE TABLE IF NOT EXISTS `serial_issue` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '到刊记录ID',
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `volume` VARCHAR(20) DEFAULT NULL COMMENT '卷',
    `issue` VARCHAR(20) DEFAULT NULL COMMENT '期',
    `expected_date` DATE DEFAULT NULL COMMENT '预期到刊日期',
    `received_date` DATE DEFAULT NULL COMMENT '实际到刊日期',
    `status` VARCHAR(20) NOT NULL DEFAULT 'EXPECTED' COMMENT '状态: EXPECTED/RECEIVED/MISSING',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_subscription_id` (`subscription_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_expected_date` (`expected_date`),
    INDEX `idx_sub_status_date` (`subscription_id`, `status`, `expected_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊到刊表';

-- ============================================================
-- 34. 期刊催缺表
-- ============================================================
CREATE TABLE IF NOT EXISTS `serial_claim` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '催缺记录ID',
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `issue_id` BIGINT DEFAULT NULL COMMENT '到刊记录ID',
    `claim_number` VARCHAR(50) NOT NULL COMMENT '催缺单号',
    `vendor_id` BIGINT DEFAULT NULL COMMENT '供应商ID',
    `claim_type` VARCHAR(20) NOT NULL DEFAULT 'MISSING' COMMENT '催缺类型: MISSING/DAMAGED/WRONG',
    `claim_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '催缺状态: PENDING/SENT/ACKNOWLEDGED/RESOLVED/CLOSED',
    `claim_date` DATE NOT NULL COMMENT '催缺日期',
    `response_date` DATE DEFAULT NULL COMMENT '供应商回复日期',
    `description` TEXT DEFAULT NULL COMMENT '问题描述',
    `resolution` TEXT DEFAULT NULL COMMENT '处理结果',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_claim_number` (`claim_number`),
    INDEX `idx_subscription_id` (`subscription_id`),
    INDEX `idx_claim_status` (`claim_status`),
    INDEX `idx_vendor_id` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊催缺表';

-- ============================================================
-- 35. 期刊路由分发表
-- ============================================================
CREATE TABLE IF NOT EXISTS `serial_routing` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分发记录ID',
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `issue_id` BIGINT DEFAULT NULL COMMENT '到刊ID',
    `destination` VARCHAR(100) NOT NULL COMMENT '分发目标',
    `copies` INT NOT NULL DEFAULT 1 COMMENT '分发份数',
    `routing_order` INT NOT NULL DEFAULT 1 COMMENT '分发顺序',
    `routing_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '分发状态: PENDING/IN_TRANSIT/DELIVERED/RETURNED',
    `sent_date` DATE DEFAULT NULL COMMENT '发出日期',
    `received_by` VARCHAR(100) DEFAULT NULL COMMENT '签收人',
    `received_date` DATE DEFAULT NULL COMMENT '签收日期',
    `notes` TEXT DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_subscription_id` (`subscription_id`),
    INDEX `idx_routing_status` (`routing_status`),
    INDEX `idx_destination` (`destination`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊路由分发表';

-- ============================================================
-- 36. 期刊路由分发模板表
-- ============================================================
CREATE TABLE IF NOT EXISTS `serial_routing_template` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `destination` VARCHAR(100) NOT NULL COMMENT '分发目标',
    `copies` INT NOT NULL DEFAULT 1 COMMENT '分发份数',
    `routing_order` INT NOT NULL DEFAULT 1 COMMENT '分发顺序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_subscription_id` (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊路由分发模板表';

SET FOREIGN_KEY_CHECKS = 1;
