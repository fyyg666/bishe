-- =========================================
-- 图书馆管理系统 - 数据库索引优化脚本
-- 目的: 为高频查询添加复合索引，提升SQL性能
-- Date: 2026-04-24
-- =========================================

USE library_system;

-- =========================================
-- 1. book表 - 图书查询优化
-- =========================================

-- 索引1: 按状态+分类查询（高频）
-- 场景: SELECT * FROM book WHERE status=0 AND category_id=?
CREATE INDEX idx_status_category ON book(status, category_id);

-- 索引2: 按作者+状态查询
-- 场景: SELECT * FROM book WHERE author LIKE '%?%' AND status=0
CREATE INDEX idx_author_status ON book(author, status);

-- 索引3: 按书名+状态查询
-- 场景: SELECT * FROM book WHERE title LIKE '%?%' AND status=0
CREATE INDEX idx_title_status ON book(title(100), status);


-- =========================================
-- 2. borrow_record表 - 借阅记录查询优化
-- =========================================

-- 索引4: 用户+状态查询（高频）
-- 场景: SELECT * FROM borrow_record WHERE user_id=? AND status='BORROWING'
CREATE INDEX idx_user_status ON borrow_record(user_id, status);

-- 索引5: 图书+状态查询
-- 场景: SELECT * FROM borrow_record WHERE book_id=? AND status='BORROWING'
CREATE INDEX idx_book_status ON borrow_record(book_id, status);

-- 索引6: 借阅日期+状态查询（统计用）
-- 场景: SELECT COUNT(*) FROM borrow_record WHERE borrow_date BETWEEN ? AND ? AND status=?
CREATE INDEX idx_borrowdate_status ON borrow_record(borrow_date, status);


-- =========================================
-- 3. seat_reservation表 - 座位预约查询优化
-- =========================================

-- 索引7: 用户+状态查询（高频）
-- 场景: SELECT * FROM seat_reservation WHERE user_id=? AND status='PENDING'
CREATE INDEX idx_user_status ON seat_reservation(user_id, status);

-- 索引8: 座位+状态查询
-- 场景: SELECT * FROM seat_reservation WHERE seat_id=? AND status='CHECKED_IN'
CREATE INDEX idx_seat_status ON seat_reservation(seat_id, status);

-- 索引9: 开始时间+状态查询（定时任务用）
-- 场景: SELECT * FROM seat_reservation WHERE start_time BETWEEN ? AND ? AND status='PENDING'
CREATE INDEX idx_starttime_status ON seat_reservation(start_time, status);


-- =========================================
-- 4. announcement表 - 公告查询优化
-- =========================================

-- 索引10: 状态+发布时间查询（高频）
-- 场景: SELECT * FROM announcement WHERE status='PUBLISHED' ORDER BY publish_time DESC
CREATE INDEX idx_status_publishtime ON announcement(status, publish_time);


-- =========================================
-- 5. sys_operation_log表 - 日志查询优化
-- =========================================

-- 索引11: 模块+创建时间查询（日志分析用）
-- 场景: SELECT * FROM sys_operation_log WHERE module=? AND create_time BETWEEN ? AND ?
CREATE INDEX idx_module_createtime ON sys_operation_log(module, create_time);

-- 索引12: 用户+创建时间查询（用户行为分析用）
-- 场景: SELECT * FROM sys_operation_log WHERE user_id=? AND create_time BETWEEN ? AND ?
CREATE INDEX idx_user_createtime ON sys_operation_log(user_id, create_time);


-- =========================================
-- 6. credit_log表 - 积分日志查询优化
-- =========================================

-- 索引13: 用户+创建时间查询（高频）
-- 场景: SELECT * FROM credit_log WHERE user_id=? ORDER BY create_time DESC
CREATE INDEX idx_user_createtime ON credit_log(user_id, create_time);

-- 索引14: 变动类型+创建时间查询（统计用）
-- 场景: SELECT * FROM credit_log WHERE change_type=? AND create_time BETWEEN ? AND ?
CREATE INDEX idx_changetype_createtime ON credit_log(change_type, create_time);


-- =========================================
-- 说明
-- =========================================
-- 1. 复合索引遵循"最左前缀"原则，查询条件需包含索引的最左列
-- 2. 索引会占用存储空间并影响写入性能，需权衡使用
-- 3. 建议定期使用 EXPLAIN 分析查询计划，验证索引有效性
-- 4. 对于低基数字段（如status），复合索引效果优于单列索引
