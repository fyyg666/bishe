-- ============================================================
-- 图书馆管理系统 V2.0 数据库结构全面整改脚本
-- 目标：从 B- 提升至 S+ 评级
-- 执行前已备份至 db-backup-20260428/
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- P0-1: 清理重复 admin 用户 + 添加 username 唯一约束
-- ============================================================
-- 先检查重复用户，保留 id=1（最早的），删除 id=30
DELETE FROM sys_user WHERE id = 30 AND username = 'admin';
-- 添加唯一约束（先删旧索引再建唯一索引）
ALTER TABLE sys_user DROP INDEX idx_username;
ALTER TABLE sys_user ADD UNIQUE KEY uk_username (username);

-- ============================================================
-- P0-2: compensation_order 添加外键约束
-- ============================================================
ALTER TABLE compensation_order
  ADD CONSTRAINT fk_comp_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  ADD CONSTRAINT fk_comp_book FOREIGN KEY (book_id) REFERENCES book(id),
  ADD CONSTRAINT fk_comp_borrow FOREIGN KEY (borrow_id) REFERENCES borrow_record(id),
  ADD CONSTRAINT fk_comp_handler FOREIGN KEY (handler_id) REFERENCES sys_user(id);

-- ============================================================
-- P0-3: notification 添加外键约束
-- ============================================================
ALTER TABLE notification
  ADD CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  ADD CONSTRAINT fk_notif_announcement FOREIGN KEY (announcement_id) REFERENCES announcement(id);

-- ============================================================
-- P1-1: 统一 notification 字符集
-- ============================================================
ALTER TABLE notification CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- ============================================================
-- P1-2: compensation_order 时间字段统一为 created_at/updated_at
-- ============================================================
ALTER TABLE compensation_order
  CHANGE COLUMN create_time created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CHANGE COLUMN update_time updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
-- 删除旧索引，更新新索引
ALTER TABLE compensation_order DROP INDEX idx_create_time;
ALTER TABLE compensation_order ADD INDEX idx_created_at (created_at);

-- ============================================================
-- P1-3: 统一 deleted 字段类型为 tinyint NOT NULL DEFAULT 0
-- ============================================================
-- compensation_order: tinyint DEFAULT 0 → tinyint NOT NULL DEFAULT 0
ALTER TABLE compensation_order
  MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0-未删除，1-已删除';

-- volunteer_service: int NOT NULL DEFAULT 0 → tinyint NOT NULL DEFAULT 0
ALTER TABLE volunteer_service
  MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除';

-- notification: tinyint DEFAULT 0 → tinyint NOT NULL DEFAULT 0
ALTER TABLE notification
  MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除';

-- ============================================================
-- P1-4: borrow_record 添加缺失的 deleted 和 remark 字段
-- ============================================================
-- borrow_record 数据库中没有 deleted 字段但实体类有，需要添加
ALTER TABLE borrow_record
  ADD COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER updated_at;

-- ============================================================
-- P1-5: notification 补充 updated_at 字段（目前只有 created_at）
-- ============================================================
ALTER TABLE notification
  ADD COLUMN updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;

-- ============================================================
-- P2-1: 删除冗余索引（UNIQUE 已包含索引功能）
-- ============================================================
-- sys_user: reader_card 有 UNIQUE + 普通索引
ALTER TABLE sys_user DROP INDEX idx_reader_card;

-- book_category: code 有 UNIQUE + 普通索引
ALTER TABLE book_category DROP INDEX idx_code;

-- reading_room: code 有 UNIQUE + 普通索引
ALTER TABLE reading_room DROP INDEX idx_code;

-- book: isbn 有 UNIQUE + 普通索引
ALTER TABLE book DROP INDEX idx_isbn;

-- notification: 已有 idx_user_status 复合索引，单列 idx_user_id 和 idx_status 冗余
ALTER TABLE notification DROP INDEX idx_user_id;
ALTER TABLE notification DROP INDEX idx_status;

-- volunteer_service: idx_deleted 索引选择性极低（只有0/1），删除
ALTER TABLE volunteer_service DROP INDEX idx_deleted;

-- ============================================================
-- P2-2: 添加高频查询复合索引
-- ============================================================
-- borrow_record: 按用户+状态查询（当前借阅）
CREATE INDEX idx_borrow_user_status ON borrow_record(user_id, status);
-- borrow_record: 按用户+应还日期查询（即将到期）
CREATE INDEX idx_borrow_user_due ON borrow_record(user_id, due_date);

-- seat_reservation: 按用户+日期+状态查询（今日预约）
CREATE INDEX idx_reservation_user_date_status ON seat_reservation(user_id, reservation_date, status);
-- seat_reservation: 按日期+座位+状态查询（某座位某天预约情况）
CREATE INDEX idx_reservation_seat_date ON seat_reservation(seat_id, reservation_date, status);

-- sys_operation_log: 按用户+时间查询
CREATE INDEX idx_oplog_user_created ON sys_operation_log(user_id, created_at);

-- compensation_order: 按用户+状态查询
CREATE INDEX idx_comp_user_status ON compensation_order(user_id, status);

-- ============================================================
-- P2-3: sys_operation_log 去除冗余字段（有重复语义的字段）
-- ============================================================
-- request_params 和 params 重复 → 保留 params
ALTER TABLE sys_operation_log DROP COLUMN request_params;
-- ip 和 ip_address 重复 → 保留 ip
ALTER TABLE sys_operation_log DROP COLUMN ip_address;
-- error_msg 和 error_message 重复 → 保留 error_message
ALTER TABLE sys_operation_log DROP COLUMN error_msg;

-- ============================================================
-- P2-4: 补充缺失字段 COMMENT
-- ============================================================
-- notification 字段 COMMENT
ALTER TABLE notification
  MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  MODIFY COLUMN user_id bigint NOT NULL COMMENT '接收用户ID',
  MODIFY COLUMN announcement_id bigint DEFAULT NULL COMMENT '关联公告ID',
  MODIFY COLUMN title varchar(200) NOT NULL COMMENT '通知标题',
  MODIFY COLUMN content text COMMENT '通知内容',
  MODIFY COLUMN type varchar(50) DEFAULT 'ANNOUNCEMENT' COMMENT '通知类型：ANNOUNCEMENT-公告通知，SYSTEM-系统通知',
  MODIFY COLUMN status varchar(20) DEFAULT 'UNREAD' COMMENT '通知状态：UNREAD-未读，READ-已读',
  MODIFY COLUMN read_at datetime DEFAULT NULL COMMENT '阅读时间';

-- volunteer_service remark 字段补 COMMENT
ALTER TABLE volunteer_service
  MODIFY COLUMN remark varchar(500) DEFAULT NULL COMMENT '审核备注';

-- ============================================================
-- P2-5: 补充缺失的表级别 COMMENT
-- ============================================================
ALTER TABLE notification COMMENT = '用户通知表';
ALTER TABLE sys_operation_log COMMENT = '系统操作日志表';

-- ============================================================
-- P2-6: 补充 seat_reservation 缺失的 updated_at 字段
-- ============================================================
-- (已有，确认不需要)

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证：输出整改后的关键状态
-- ============================================================
SELECT '=== 验证 username 唯一约束 ===' AS info;
SHOW INDEX FROM sys_user WHERE Column_name = 'username';

SELECT '=== 验证 compensation_order 外键 ===' AS info;
SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'library_system' AND TABLE_NAME = 'compensation_order' AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SELECT '=== 验证 notification 外键 ===' AS info;
SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'library_system' AND TABLE_NAME = 'notification' AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SELECT '=== 验证 deleted 类型统一 ===' AS info;
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'library_system' AND COLUMN_NAME = 'deleted'
ORDER BY TABLE_NAME;

SELECT '=== 验证时间字段命名统一 ===' AS info;
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'library_system'
AND COLUMN_NAME IN ('create_time', 'update_time')
ORDER BY TABLE_NAME;

SELECT '=== 验证字符集统一 ===' AS info;
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'library_system'
AND TABLE_COLLATION != 'utf8mb4_0900_ai_ci'
ORDER BY TABLE_NAME;

SELECT '=== 验证冗余索引已删除 ===' AS info;
SELECT TABLE_NAME, INDEX_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'library_system'
AND INDEX_NAME IN ('idx_reader_card', 'idx_code', 'idx_isbn', 'idx_create_time', 'idx_deleted')
ORDER BY TABLE_NAME;

SELECT '=== 整改完成 ===' AS status;
