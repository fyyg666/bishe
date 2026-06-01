-- =========================================
-- 图书馆管理系统 - 大表分区脚本
-- 目的: 对大表进行范围分区，提升查询性能和管理效率
-- Date: 2026-04-24
-- =========================================
-- 注意: 执行此脚本前请先备份数据！
-- =========================================
-- 重要说明: 分区操作为可选优化项，当前所有ALTER TABLE PARTITION语句
-- 均已注释。启用分区前需评估：1) 数据量是否达到分区阈值(>100万行)
-- 2) 查询模式是否匹配分区键 3) 运维团队是否具备分区管理能力
-- 启用方法：取消下方对应ALTER TABLE语句的注释后执行
-- =========================================

USE library_system;

-- =========================================
-- 1. borrow_record 表分区
-- 分区策略: 按借阅日期 RANGE 分区
-- 保留最近3年数据
-- =========================================

-- 步骤1: 创建归档表（用于存储历史数据）
CREATE TABLE IF NOT EXISTS borrow_record_archive LIKE borrow_record;

-- 步骤2: 添加分区（MySQL 8.0+支持）
-- 如果表已有数据，需要先移除外键约束，然后重新创建分区表

-- 检查当前表引擎
-- ALTER TABLE borrow_record ENGINE = InnoDB;

-- 对于已有数据的表，使用以下方法迁移到分区表：
-- 方法1: 直接转换（MySQL 8.0.17+支持在线DDL）
-- ALTER TABLE borrow_record PARTITION BY RANGE (TO_DAYS(borrow_date)) (
--     PARTITION p2024q1 VALUES LESS THAN (TO_DAYS('2024-04-01')),
--     PARTITION p2024q2 VALUES LESS THAN (TO_DAYS('2024-07-01')),
--     PARTITION p2024q3 VALUES LESS THAN (TO_DAYS('2024-10-01')),
--     PARTITION p2024q4 VALUES LESS THAN (TO_DAYS('2025-01-01')),
--     PARTITION p2025q1 VALUES LESS THAN (TO_DAYS('2025-04-01')),
--     PARTITION p2025q2 VALUES LESS THAN (TO_DAYS('2025-07-01')),
--     PARTITION p2025q3 VALUES LESS THAN (TO_DAYS('2025-10-01')),
--     PARTITION p2025q4 VALUES LESS THAN (TO_DAYS('2026-01-01')),
--     PARTITION p2026q1 VALUES LESS THAN (TO_DAYS('2026-04-01')),
--     PARTITION p2026q2 VALUES LESS THAN (TO_DAYS('2026-07-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- 方法2: 如果表数据量较大，建议使用 pt-online-schema-change
-- pt-online-schema-change --alter "PARTITION BY RANGE (TO_DAYS(borrow_date)) (...)" D=library_system t=borrow_record


-- =========================================
-- 2. sys_operation_log 表分区
-- 分区策略: 按创建时间 RANGE 分区
-- 保留最近6个月数据
-- =========================================

-- 步骤1: 创建归档表
CREATE TABLE IF NOT EXISTS sys_operation_log_archive LIKE sys_operation_log;

-- 步骤2: 分区配置（按月分区）
-- ALTER TABLE sys_operation_log PARTITION BY RANGE (TO_DAYS(create_time)) (
--     PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
--     PARTITION p202604 VALUES LESS THAN (TO_DAYS('2026-05-01')),
--     PARTITION p202605 VALUES LESS THAN (TO_DAYS('2026-06-01')),
--     PARTITION p202606 VALUES LESS THAN (TO_DAYS('2026-07-01')),
--     PARTITION p202607 VALUES LESS THAN (TO_DAYS('2026-08-01')),
--     PARTITION p202608 VALUES LESS THAN (TO_DAYS('2026-09-01')),
--     PARTITION p202609 VALUES LESS THAN (TO_DAYS('2026-10-01')),
--     PARTITION p202610 VALUES LESS THAN (TO_DAYS('2026-11-01')),
--     PARTITION p202611 VALUES LESS THAN (TO_DAYS('2026-12-01')),
--     PARTITION p202612 VALUES LESS THAN (TO_DAYS('2027-01-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );


-- =========================================
-- 3. seat_reservation 表分区
-- 分区策略: 按预约开始时间 RANGE 分区
-- =========================================

-- ALTER TABLE seat_reservation PARTITION BY RANGE (TO_DAYS(start_time)) (
--     PARTITION p2026q2 VALUES LESS THAN (TO_DAYS('2026-07-01')),
--     PARTITION p2026q3 VALUES LESS THAN (TO_DAYS('2026-10-01')),
--     PARTITION p2026q4 VALUES LESS THAN (TO_DAYS('2027-01-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );


-- =========================================
-- 4. credit_log 表分区
-- 分区策略: 按创建时间 RANGE 分区
-- =========================================

-- ALTER TABLE credit_log PARTITION BY RANGE (TO_DAYS(create_time)) (
--     PARTITION p2026q2 VALUES LESS THAN (TO_DAYS('2026-07-01')),
--     PARTITION p2026q3 VALUES LESS THAN (TO_DAYS('2026-10-01')),
--     PARTITION p2026q4 VALUES LESS THAN (TO_DAYS('2027-01-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );


-- =========================================
-- 5. 分区管理 - 历史数据归档
-- =========================================

-- 示例: 归档6个月前的借阅记录
-- INSERT INTO borrow_record_archive SELECT * FROM borrow_record WHERE borrow_date < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);
-- DELETE FROM borrow_record WHERE borrow_date < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);

-- 示例: 归档1个月前的操作日志
-- INSERT INTO sys_operation_log_archive SELECT * FROM sys_operation_log WHERE create_time < DATE_SUB(CURDATE(), INTERVAL 1 MONTH);
-- DELETE FROM sys_operation_log WHERE create_time < DATE_SUB(CURDATE(), INTERVAL 1 MONTH);


-- =========================================
-- 6. 分区维护任务（建议添加到 MySQL Event Scheduler）
-- =========================================

-- 启用事件调度器（需要 SUPER 权限）
-- SET GLOBAL event_scheduler = ON;

-- 创建自动归档存储过程
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS archive_old_data()
BEGIN
    -- 归档6个月前的借阅记录
    INSERT IGNORE INTO borrow_record_archive 
    SELECT * FROM borrow_record 
    WHERE borrow_date < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);
    
    DELETE FROM borrow_record 
    WHERE borrow_date < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);
    
    -- 归档1个月前的操作日志
    INSERT IGNORE INTO sys_operation_log_archive 
    SELECT * FROM sys_operation_log 
    WHERE create_time < DATE_SUB(CURDATE(), INTERVAL 1 MONTH);
    
    DELETE FROM sys_operation_log 
    WHERE create_time < DATE_SUB(CURDATE(), INTERVAL 1 MONTH);
    
    -- 归档3个月前的座位预约记录
    INSERT IGNORE INTO seat_reservation_archive 
    SELECT * FROM seat_reservation 
    WHERE start_time < DATE_SUB(CURDATE(), INTERVAL 3 MONTH);
    
    DELETE FROM seat_reservation 
    WHERE start_time < DATE_SUB(CURDATE(), INTERVAL 3 MONTH);
    
    -- 归档6个月前的积分日志
    INSERT IGNORE INTO credit_log_archive 
    SELECT * FROM credit_log 
    WHERE create_time < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);
    
    DELETE FROM credit_log 
    WHERE create_time < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);
END //

DELIMITER ;

-- 创建自动归档事件（每月执行一次）
-- CREATE EVENT IF NOT EXISTS monthly_archive_event
-- ON SCHEDULE EVERY 1 MONTH
-- STARTS CURRENT_TIMESTAMP
-- DO CALL archive_old_data();


-- =========================================
-- 7. 查看分区状态
-- =========================================

-- 查看表的分区信息
-- SELECT 
--     PARTITION_NAME,
--     PARTITION_ORDINAL_POSITION,
--     PARTITION_METHOD,
--     PARTITION_EXPRESSION,
--     PARTITION_DESCRIPTION,
--     TABLE_ROWS
-- FROM INFORMATION_SCHEMA.PARTITIONS
-- WHERE TABLE_SCHEMA = 'library_system' 
--   AND TABLE_NAME = 'borrow_record'
-- ORDER BY PARTITION_ORDINAL_POSITION;

-- 查看分区统计
-- SELECT 
--     PARTITION_NAME,
--     TABLE_ROWS,
--     AVG_ROW_LENGTH,
--     DATA_LENGTH
-- FROM INFORMATION_SCHEMA.PARTITIONS
-- WHERE TABLE_SCHEMA = 'library_system'
-- ORDER BY PARTITION_NAME;


-- =========================================
-- 8. 注意事项
-- =========================================
-- 1. 执行分区前务必先备份数据！
-- 2. 分区后，旧数据的清理更加高效（使用 DROP PARTITION）
-- 3. 建议在低峰期执行分区操作
-- 4. 定期监控分区使用情况，及时添加新分区
-- 5. 分区列必须是主键的一部分（如果表有主键）
-- 6. 考虑使用 pt-online-schema-change 工具进行在线DDL
