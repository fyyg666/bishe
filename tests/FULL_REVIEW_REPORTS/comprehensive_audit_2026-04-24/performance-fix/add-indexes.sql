-- 图书馆管理系统V2.0 - 数据库索引优化脚本
-- 执行日期: 2026-04-24
-- 说明: 添加缺少的复合索引，优化高频查询性能

USE library_system;

-- =========================================
-- 1. 借阅记录表优化
-- =========================================

-- 1.1 按用户+状态查询（常用查询：查询我的借阅记录）
CREATE INDEX idx_user_status ON borrow_record(user_id, status);

-- 1.2 按状态+应还日期查询（逾期查询：查找所有逾期记录）
CREATE INDEX idx_status_due_date ON borrow_record(status, due_date);

-- 1.3 按图书+状态查询（热门图书统计：统计图书借阅次数）
CREATE INDEX idx_book_status ON borrow_record(book_id, status);

-- 1.4 覆盖索引：避免回表查询（如果只需要user_id, status, borrow_date）
-- CREATE INDEX idx_user_status_date_cover ON borrow_record(user_id, status, borrow_date, id);


-- =========================================
-- 2. 座位预约表优化
-- =========================================

-- 2.1 按用户+状态+开始时间查询（用户预约记录）
CREATE INDEX idx_user_status_time ON seat_reservation(user_id, status, start_time);

-- 2.2 按座位+时间段查询（冲突检测：检查座位是否已被预约）
CREATE INDEX idx_seat_time ON seat_reservation(seat_id, start_time, end_time, status);

-- 2.3 按阅览室+时间段查询（阅览室预约情况）
CREATE INDEX idx_room_time ON seat_reservation(room_id, start_time, end_time, status);


-- =========================================
-- 3. 积分日志表优化
-- =========================================

-- 3.1 按用户+创建时间查询（积分明细：查看用户积分变动历史）
CREATE INDEX idx_user_time ON credit_log(user_id, create_time);

-- 3.2 按变动类型+创建时间查询（统计分析：按类型统计积分变动）
CREATE INDEX idx_type_time ON credit_log(change_type, create_time);


-- =========================================
-- 4. 志愿服务表优化
-- =========================================

-- 4.1 按用户+状态+服务日期查询（志愿服务记录）
CREATE INDEX idx_vol_user_status_date ON volunteer_service(user_id, status, service_date);

-- 4.2 按状态+服务日期查询（审核：查找待审核的志愿服务）
CREATE INDEX idx_status_date ON volunteer_service(status, service_date);


-- =========================================
-- 5. 操作日志表优化
-- =========================================

-- 5.1 按模块+操作+时间查询（日志审计）
CREATE INDEX idx_module_op_time ON sys_operation_log(module, operation, create_time);

-- 5.2 按用户+时间查询（用户行为分析）
CREATE INDEX idx_user_time ON sys_operation_log(user_id, create_time);


-- =========================================
-- 6. 图书表优化
-- =========================================

-- 6.1 按分类+状态查询（分类浏览：查找某分类下的可借图书）
CREATE INDEX idx_category_status ON book(category_id, status);

-- 6.2 按作者+状态查询（作者作品：查找某作者的所有可借图书）
CREATE INDEX idx_author_status ON book(author, status);


-- =========================================
-- 7. 用户表优化
-- =========================================

-- 7.1 按角色+状态查询（用户管理：查找某角色的所有正常用户）
CREATE INDEX idx_role_status ON sys_user(role, status);

-- 7.2 按积分范围查询（积分排名：查找积分最高的用户）
CREATE INDEX idx_credit ON sys_user(credit_score DESC);


-- =========================================
-- 8. 验证索引创建成功
-- =========================================

-- 查看所有表的索引
SHOW INDEX FROM borrow_record;
SHOW INDEX FROM seat_reservation;
SHOW INDEX FROM credit_log;
SHOW INDEX FROM volunteer_service;
SHOW INDEX FROM sys_operation_log;
SHOW INDEX FROM book;
SHOW INDEX FROM sys_user;

-- =========================================
-- 9. 分析查询计划（示例）
-- =========================================

-- 9.1 查询我的借阅记录（应使用 idx_user_status）
EXPLAIN SELECT * FROM borrow_record WHERE user_id = 1 AND status = 'BORROWING';

-- 9.2 查询逾期记录（应使用 idx_status_due_date）
EXPLAIN SELECT * FROM borrow_record WHERE status = 'BORROWING' AND due_date < NOW();

-- 9.3 查询用户预约记录（应使用 idx_user_status_time）
EXPLAIN SELECT * FROM seat_reservation WHERE user_id = 1 AND status = 'PENDING' ORDER BY start_time DESC;

-- 9.4 检测座位冲突（应使用 idx_seat_time）
EXPLAIN SELECT * FROM seat_reservation 
WHERE seat_id = 1 
  AND status IN ('PENDING', 'CHECKED_IN')
  AND ((start_time <= '2026-04-24 10:00:00' AND end_time > '2026-04-24 10:00:00')
    OR (start_time >= '2026-04-24 10:00:00' AND start_time < '2026-04-24 12:00:00'));


-- =========================================
-- 10. 索引维护建议
-- =========================================

-- 10.1 定期分析表（更新索引统计信息）
ANALYZE TABLE borrow_record;
ANALYZE TABLE seat_reservation;
ANALYZE TABLE credit_log;
ANALYZE TABLE volunteer_service;
ANALYZE TABLE sys_operation_log;

-- 10.2 监控索引使用情况（MySQL 8.0+）
SELECT 
    OBJECT_SCHEMA,
    OBJECT_NAME,
    INDEX_NAME,
    COUNT_READ,
    COUNT_WRITE,
    COUNT_FETCH,
    COUNT_INSERT,
    COUNT_UPDATE,
    COUNT_DELETE
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'library_system'
ORDER BY COUNT_READ DESC;


-- =========================================
-- 说明
-- =========================================

-- 1. 执行前请备份数据库：mysqldump -u root -p library_system > backup.sql
-- 2. 大表添加索引可能需要较长时间，请在低峰期执行
-- 3. 可以使用 ALGORITHM=INPLACE, LOCK=NONE 来减少锁表时间（MySQL 5.6+）
--    示例：ALTER TABLE borrow_record ADD INDEX idx_user_status (user_id, status), ALGORITHM=INPLACE, LOCK=NONE;
-- 4. 执行完成后，使用 EXPLAIN 验证查询是否使用了索引
-- 5. 定期（每周）分析慢查询日志，持续优化索引
