-- =========================================
-- 数据库迁移脚本
-- 新增 sys_user 缺失列: violation_count, ban_until
-- 日期: 2026-05-10
-- =========================================

-- 添加违约次数列
ALTER TABLE sys_user
ADD COLUMN violation_count INT DEFAULT 0 COMMENT '违约次数'
AFTER borrow_count;

-- 添加封禁到期时间列
ALTER TABLE sys_user
ADD COLUMN ban_until DATETIME COMMENT '封禁到期时间'
AFTER violation_count;

COMMIT;
