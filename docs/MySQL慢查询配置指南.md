/**
 * MySQL 慢查询日志配置指南
 * 
 * 本文档说明如何配置 MySQL 慢查询日志，用于性能分析和优化
 * 
 * 配置时间: 2026-04-24
 */

# =========================================
# 1. 配置文件位置
# =========================================

# Linux: /etc/mysql/my.cnf 或 /etc/my.cnf
# Windows: MySQL安装目录下的 my.ini

# =========================================
# 2. 启用慢查询日志
# =========================================

[mysqld]
# 启用慢查询日志
slow_query_log = 1

# 慢查询日志文件路径
slow_query_log_file = /var/log/mysql/slow.log

# 慢查询阈值（秒），超过此时间的查询将被记录
long_query_time = 1

# 记录没有使用索引的查询
log_queries_not_using_indexes = 1

# 记录查询时间超过 long_query_time 的查询
log_slow_admin_statements = 1

# 记录慢查询的数量
log_slow_slave_statements = 1

# =========================================
# 3. Windows 配置示例 (my.ini)
# =========================================

# [mysqld]
# slow_query_log = 1
# slow_query_log_file = "C:/ProgramData/MySQL/MySQL Server 8.0/Data/slow.log"
# long_query_time = 1
# log_queries_not_using_indexes = 1

# =========================================
# 4. 临时启用（不需要重启）
# =========================================

-- 临时启用慢查询日志
SET GLOBAL slow_query_log = 'ON';

-- 设置慢查询时间阈值（秒）
SET GLOBAL long_query_time = 1;

-- 设置日志文件路径
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 查看当前配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

# =========================================
# 5. 分析慢查询日志
# =========================================

# 使用 mysqldumpslow 工具分析日志（Linux）
mysqldumpslow -t 10 /var/log/mysql/slow.log

# 参数说明:
# -t N: 显示前 N 条最慢的查询
# -s: 排序方式 (c=次数, t=时间, l=锁定时间)
# -g: 按模式过滤

# 使用 pt-query-digest 分析（需要安装 Percona Toolkit）
pt-query-digest /var/log/mysql/slow.log

# =========================================
# 6. 示例慢查询日志内容
# =========================================

# Time: 2026-04-24T10:30:00.123456Z
# User@Host: root[root] @ localhost []
# Query_time: 2.345678  Lock_time: 0.000123 Rows_sent: 100  Rows_examined: 50000
# SELECT * FROM borrow_record WHERE user_id = 123 AND status = 'BORROWING';

# =========================================
# 7. 优化建议
# =========================================

# 根据慢查询日志分析结果:
# 1. 识别高频查询 → 添加索引
# 2. 识别全表扫描 → 优化 SQL 语句
# 3. 识别大结果集 → 分页处理
# 4. 识别锁等待 → 调整事务隔离级别

# =========================================
# 8. 日志轮转配置
# =========================================

# Linux: 使用 logrotate
# /etc/logrotate.d/mysql
"""
/var/log/mysql/slow.log {
    daily
    rotate 7
    missingok
    notifempty
    create 640 mysql mysql
    postrotate
        mysqladmin flush-logs
    endscript
}
"""

# =========================================
# 9. 监控告警
# =========================================

# 可以使用以下 SQL 查看慢查询统计
SELECT 
    start_time,
    query_time,
    rows_sent,
    rows_examined,
    sql_text
FROM mysql.slow_log
ORDER BY start_time DESC
LIMIT 10;

# =========================================
# 10. 注意事项
# =========================================

# 1. 慢查询日志会占用磁盘空间，需要定期清理或轮转
# 2. 启用 log_queries_not_using_indexes 可能会产生大量日志
# 3. 建议在生产环境谨慎使用，结合业务高峰时段分析
# 4. 定期分析日志，识别性能问题并优化
