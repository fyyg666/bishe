-- =========================================
-- 图书馆管理系统 - 数据库初始化脚本
-- Version: 2.0.0
-- Database: MySQL 8.0
-- =========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS library_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_system;

-- =========================================
-- 1. 用户表
-- =========================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'READER' COMMENT '角色: ADMIN/LIBRARIAN/READER/VOLUNTEER',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL/DISABLED/LOCKED',
    credit_score INT DEFAULT 100 COMMENT '积分',
    card_number VARCHAR(20) COMMENT '读者卡号',
    borrow_count INT DEFAULT 0 COMMENT '当前借阅数量',
    violation_count INT DEFAULT 0 COMMENT '违约次数',
    ban_until DATETIME COMMENT '封禁到期时间',
    version INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_username (username),
    INDEX idx_card_number (card_number),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =========================================
-- 2. 图书分类表
-- =========================================
DROP TABLE IF EXISTS book_category;
CREATE TABLE book_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) COMMENT '分类编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书分类表';

-- =========================================
-- 3. 图书表
-- =========================================
DROP TABLE IF EXISTS book;
CREATE TABLE book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图书ID',
    isbn VARCHAR(20) NOT NULL UNIQUE COMMENT 'ISBN',
    title VARCHAR(200) NOT NULL COMMENT '书名',
    author VARCHAR(100) COMMENT '作者',
    category_id BIGINT COMMENT '分类ID',
    publisher VARCHAR(100) COMMENT '出版社',
    publish_date DATE COMMENT '出版日期',
    price DECIMAL(10,2) COMMENT '价格',
    total_quantity INT DEFAULT 1 COMMENT '总数量',
    stock INT DEFAULT 1 COMMENT '当前库存',
    borrow_count INT DEFAULT 0 COMMENT '借阅次数',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-正常, 1-下架',
    description TEXT COMMENT '描述',
    cover_image VARCHAR(255) COMMENT '封面图片URL',
    version INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_isbn (isbn),
    INDEX idx_category_id (category_id),
    INDEX idx_title (title),
    INDEX idx_author (author),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_title_author (title, author)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书表';

-- =========================================
-- 4. 借阅记录表
-- =========================================
DROP TABLE IF EXISTS borrow_record;
CREATE TABLE borrow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '借阅记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    borrow_date DATETIME NOT NULL COMMENT '借阅日期',
    due_date DATETIME NOT NULL COMMENT '应归还日期',
    return_date DATETIME COMMENT '实际归还日期',
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWING' COMMENT '状态: BORROWING/RETURNED/OVERDUE',
    renew_count INT DEFAULT 0 COMMENT '续借次数',
    fine_amount DECIMAL(10,2) DEFAULT 0 COMMENT '罚款金额',
    version INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    INDEX idx_borrow_date (borrow_date),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅记录表';

-- =========================================
-- 5. 阅览室表
-- =========================================
DROP TABLE IF EXISTS reading_room;
CREATE TABLE reading_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '阅览室ID',
    name VARCHAR(100) NOT NULL COMMENT '阅览室名称',
    location VARCHAR(200) COMMENT '位置描述',
    total_seats INT DEFAULT 0 COMMENT '总座位数',
    open_time VARCHAR(20) DEFAULT '08:00' COMMENT '开放时间',
    close_time VARCHAR(20) DEFAULT '22:00' COMMENT '关闭时间',
    status VARCHAR(20) DEFAULT 'OPEN' COMMENT '状态: OPEN/CLOSED/MAINTENANCE',
    description TEXT COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅览室表';

-- =========================================
-- 6. 座位表
-- =========================================
DROP TABLE IF EXISTS seat;
CREATE TABLE seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '座位ID',
    room_id BIGINT NOT NULL COMMENT '阅览室ID',
    seat_number VARCHAR(20) NOT NULL COMMENT '座位编号',
    location VARCHAR(100) COMMENT '位置描述',
    status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE/OCCUPIED/RESERVED/MAINTENANCE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_room_id (room_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位表';

-- =========================================
-- 7. 座位预约表
-- =========================================
DROP TABLE IF EXISTS seat_reservation;
CREATE TABLE seat_reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    room_id BIGINT NOT NULL COMMENT '阅览室ID',
    seat_id BIGINT NOT NULL COMMENT '座位ID',
    reservation_date DATETIME COMMENT '预约日期',
    start_time DATETIME NOT NULL COMMENT '预约开始时间',
    end_time DATETIME NOT NULL COMMENT '预约结束时间',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/CHECKED_IN/COMPLETED/CANCELLED/VIOLATED',
    check_in_time DATETIME COMMENT '签到时间',
    check_out_time DATETIME COMMENT '签退时间',
    violation_count INT DEFAULT 0 COMMENT '违约次数',
    version INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_room_id (room_id),
    INDEX idx_seat_id (seat_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位预约表';

-- =========================================
-- 8. 积分日志表
-- =========================================
DROP TABLE IF EXISTS credit_log;
CREATE TABLE credit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    credit_change INT NOT NULL COMMENT '变动积分',
    credit_balance INT NOT NULL COMMENT '变动后余额',
    change_type VARCHAR(50) NOT NULL COMMENT '变动类型',
    biz_id VARCHAR(50) COMMENT '相关业务ID',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_change_type (change_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分日志表';

-- =========================================
-- 9. 公告表
-- =========================================
DROP TABLE IF EXISTS announcement;
CREATE TABLE announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    type VARCHAR(20) DEFAULT 'NOTICE' COMMENT '类型: NOTICE/ACTIVITY/SYSTEM',
    priority INT DEFAULT 0 COMMENT '优先级',
    publisher_id BIGINT COMMENT '发布人ID',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ARCHIVED',
    publish_time DATETIME COMMENT '发布时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- =========================================
-- 10. 志愿服务表
-- =========================================
DROP TABLE IF EXISTS volunteer_service;
CREATE TABLE volunteer_service (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    service_date DATETIME NOT NULL COMMENT '服务日期',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    service_hours DECIMAL(5,2) NOT NULL COMMENT '服务时长(小时)',
    service_type VARCHAR(50) COMMENT '服务类型',
    description TEXT COMMENT '服务描述',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/CANCELLED',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_time DATETIME COMMENT '审核时间',
    review_remark VARCHAR(255) COMMENT '审核备注',
    version INT DEFAULT 0 COMMENT '版本号(乐观锁)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_service_date (service_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿服务表';

-- =========================================
-- 11. 操作日志表
-- =========================================
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    module VARCHAR(50) COMMENT '操作模块',
    operation VARCHAR(100) COMMENT '操作类型',
    method VARCHAR(255) COMMENT '目标方法名',
    params TEXT COMMENT '请求参数(JSON)',
    result TEXT COMMENT '返回结果(JSON)',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    ip VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(100) COMMENT '操作地点',
    duration BIGINT COMMENT '执行时长(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_module (module),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time),
    INDEX idx_operation (operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =========================================
-- 初始化数据
-- =========================================

-- 插入默认管理员账户 (密码: admin123)
INSERT INTO sys_user (username, password, real_name, role, status, credit_score) VALUES
('admin', '$2a$10$QiOOZxFl4HMIUZ2SI4IOCeFNURPoVDf/mzISxZxJegX1MGNTecq6W', '系统管理员', 'ADMIN', 'NORMAL', 100);

-- 插入图书分类
INSERT INTO book_category (name, code, parent_id, sort_order) VALUES
('计算机', 'CS', 0, 1),
('文学', 'LIT', 0, 2),
('历史', 'HIS', 0, 3),
('经济', 'ECO', 0, 4),
('哲学', 'PHI', 0, 5),
('编程语言', 'CS-PL', 1, 1),
('数据结构与算法', 'CS-DSA', 1, 2),
('数据库', 'CS-DB', 1, 3);

-- 插入阅览室
INSERT INTO reading_room (name, location, total_seats, open_time, close_time, status) VALUES
('一楼自习室', '图书馆一楼', 100, '08:00', '22:00', 'OPEN'),
('二楼阅览室', '图书馆二楼', 80, '08:00', '22:00', 'OPEN'),
('三楼电子阅览室', '图书馆三楼', 50, '08:00', '21:00', 'OPEN');

-- 插入座位（示例数据）
INSERT INTO seat (room_id, seat_number, location, status) VALUES
(1, 'A-01', 'A区-01', 'AVAILABLE'),
(1, 'A-02', 'A区-02', 'AVAILABLE'),
(1, 'B-01', 'B区-01', 'AVAILABLE'),
(2, 'A-01', 'A区-01', 'AVAILABLE'),
(2, 'A-02', 'A区-02', 'AVAILABLE'),
(3, 'PC-01', '电脑01', 'AVAILABLE'),
(3, 'PC-02', '电脑02', 'AVAILABLE');

-- 插入示例图书
INSERT INTO book (isbn, title, author, category_id, publisher, publish_date, price, total_quantity, stock, description) VALUES
('9787111213826', 'Java编程思想', 'Bruce Eckel', 6, '机械工业出版社', '2007-06-01', 108.00, 5, 5, 'Java编程经典著作'),
('9787111547537', '深入理解Java虚拟机', '周志明', 6, '机械工业出版社', '2019-06-01', 129.00, 3, 3, 'JVM深入解析'),
('9787111470660', 'Effective Java', 'Joshua Bloch', 6, '机械工业出版社', '2015-07-01', 89.00, 4, 4, 'Java最佳实践'),
('9787302147003', '算法导论', 'Thomas H. Cormen', 7, '清华大学出版社', '2012-01-01', 128.00, 3, 3, '算法经典教材'),
('9787121355474', 'MySQL必知必会', 'Ben Forta', 8, '人民邮电出版社', '2019-03-01', 29.80, 10, 10, 'MySQL入门经典');

-- 插入示例公告
INSERT INTO announcement (title, content, type, priority, status, publish_time) VALUES
('图书馆开放时间调整通知', '自即日起，图书馆开放时间调整为8:00-22:00，请各位读者知悉。', 'NOTICE', 1, 'PUBLISHED', NOW()),
('新增自习室公告', '图书馆四楼新增自习室，已正式开放使用，欢迎读者前来学习。', 'NOTICE', 2, 'PUBLISHED', NOW()),
('志愿服务招募通知', '图书馆现招募志愿者，有意者请到服务台登记。', 'ACTIVITY', 3, 'PUBLISHED', NOW());

COMMIT;

-- =========================================
-- 12. 赔偿订单表（由Compensation实体映射）
-- =========================================
DROP TABLE IF EXISTS compensation_order;
CREATE TABLE compensation_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '赔偿ID',
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '赔偿单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT COMMENT '图书ID',
    borrow_record_id BIGINT COMMENT '关联借阅记录ID',
    type VARCHAR(20) NOT NULL COMMENT '赔偿类型: LOST/DAMAGE',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '赔偿金额',
    payment_method VARCHAR(20) COMMENT '支付方式: CASH/CREDIT/VOLUNTEER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PAID/CANCELLED',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赔偿订单表';

-- =========================================
-- 13. 日统计数据表（由StatisticsDaily实体映射）
-- =========================================
DROP TABLE IF EXISTS statistics_daily;
CREATE TABLE statistics_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    stat_date DATE NOT NULL UNIQUE COMMENT '统计日期',
    total_borrows INT DEFAULT 0 COMMENT '总借阅数',
    total_returns INT DEFAULT 0 COMMENT '总归还数',
    total_readers INT DEFAULT 0 COMMENT '总读者数',
    total_books INT DEFAULT 0 COMMENT '总图书数',
    active_readers INT DEFAULT 0 COMMENT '活跃读者数',
    new_books INT DEFAULT 0 COMMENT '新增图书数',
    new_readers INT DEFAULT 0 COMMENT '新增读者数',
    total_reservations INT DEFAULT 0 COMMENT '总预约数',
    checked_in_reservations INT DEFAULT 0 COMMENT '签到预约数',
    violation_count INT DEFAULT 0 COMMENT '违约次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日统计数据表';

-- =========================================
-- 14. 系统配置表（HolidayUtil读取寒暑假配置）
-- =========================================
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- =========================================
-- 初始化数据 - 系统配置
-- =========================================
INSERT INTO sys_config (config_key, config_value, description) VALUES
('summer_holiday_2026', '2026-07-01,2026-08-31', '2026年暑假'),
('winter_holiday_2026', '2026-01-15,2026-02-25', '2026年寒假');

-- =========================================
-- 附录A：数据库分区策略说明
-- =========================================
-- 
-- 本系统数据库采用 MySQL 8.0，支持表分区以优化大数据量查询性能。
-- 
-- 一、分区策略
-- 
-- 1. 按时间范围分区（适用于高频写入的表）
--    适用表：borrow_record（借阅记录）、seat_reservation（座位预约）、credit_log（积分日志）
--    策略：按月或季度分区，便于清理历史数据和范围查询优化
--    
--    示例（借阅记录表）：
--    CREATE TABLE borrow_record (...) PARTITION BY RANGE (TO_DAYS(borrow_date)) (
--        PARTITION p2024q1 VALUES LESS THAN (TO_DAYS('2024-04-01')),
--        PARTITION p2024q2 VALUES LESS THAN (TO_DAYS('2024-07-01')),
--        PARTITION p2024q3 VALUES LESS THAN (TO_DAYS('2024-10-01')),
--        PARTITION p2024q4 VALUES LESS THAN (TO_DAYS('2025-01-01')),
--        PARTITION p_future VALUES LESS THAN MAXVALUE
--    );
--
-- 2. 按哈希分区（适用于均匀分布的表）
--    适用表：sys_user（用户表）、book（图书表）
--    策略：按用户ID或图书ID的哈希值分区
--    
--    示例：
--    CREATE TABLE book (...) PARTITION BY HASH(id) PARTITIONS 8;
--
-- 3. 列表分区（适用于枚举值固定的表）
--    适用表：announcement（公告表，按类型分区）
--    
--    示例：
--    CREATE TABLE announcement (...) PARTITION BY LIST (type) (
--        PARTITION p_notice VALUES IN ('NOTICE'),
--        PARTITION p_activity VALUES IN ('ACTIVITY'),
--        PARTITION p_system VALUES IN ('SYSTEM')
--    );
--
-- 二、分区维护操作
--
-- 1. 添加新分区（按时间范围）
--    ALTER TABLE borrow_record ADD PARTITION (
--        PARTITION p2025q1 VALUES LESS THAN (TO_DAYS('2025-04-01'))
--    );
--
-- 2. 删除过期分区（同时清理数据）
--    ALTER TABLE borrow_record DROP PARTITION p2024q1;
--
-- 3. 重建分区（优化性能）
--    ALTER TABLE borrow_record REBUILD PARTITION p2024q2;
--
-- 4. 分析分区（更新统计信息）
--    ALTER TABLE borrow_record ANALYZE PARTITION p2024q2;
--
-- 三、分区索引设计
--
-- 每个分区应创建独立的本地索引，而非全局索引。
-- 对于复合索引，分区键应在索引中作为前导列。
--
-- 示例：
-- CREATE INDEX idx_borrow_user_date ON borrow_record (user_id, borrow_date) LOCAL;
--
-- 四、分区表查询优化
--
-- 1. 查询时指定分区键，可跳过不相关分区
--    SELECT * FROM borrow_record WHERE borrow_date BETWEEN '2024-01-01' AND '2024-03-31';
--
-- 2. 避免跨分区查询（性能较差）
--    SELECT * FROM borrow_record WHERE YEAR(borrow_date) = 2024; -- 不推荐
--
-- 五、推荐初始分区方案（MySQL 8.0+）
--
-- 对于生产环境，建议按以下方式为关键表添加分区：
--
-- borrow_record: 按月分区，保留最近12个月数据
-- seat_reservation: 按月分区，保留最近3个月数据
-- credit_log: 按月分区，保留最近24个月数据
-- sys_operation_log: 按月分区，保留最近6个月数据
--
-- 六、分区迁移脚本
--
-- 在业务量增长到需要分区时，使用以下流程：
-- 1. 创建带分区的临时表
-- 2. 使用 INSERT INTO ... SELECT 将数据迁移
-- 3. 重命名原表为 _old
-- 4. 重命名新表为原表名
-- 5. 验证数据一致性后删除旧表
--
-- =========================================
-- 附录B：性能优化建议
-- =========================================
--
-- 1. 定期 ANALYZE TABLE 更新统计信息
-- 2. 使用 EXPLAIN 分析查询计划
-- 3. 监控慢查询日志（slow_query_log）
-- 4. 定期 OPTIMIZE TABLE 整理碎片空间
-- 5. 使用覆盖索引减少回表查询
--
-- =========================================
