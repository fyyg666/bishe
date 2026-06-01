CREATE TABLE IF NOT EXISTS report_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '报表名称',
    description VARCHAR(500) COMMENT '描述',
    sql_template TEXT NOT NULL COMMENT 'SQL模板',
    parameters JSON COMMENT '参数定义: [{"name":"startDate","label":"开始日期","type":"date"},...]',
    category VARCHAR(50) COMMENT '分类',
    created_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义报表模板表';

INSERT INTO report_template (name, description, sql_template, parameters, category, created_by) VALUES
('月度借阅统计', '按月统计借阅数量',
 'SELECT DATE_FORMAT(borrow_date, ''%Y-%m'') as month, COUNT(*) as borrow_count FROM borrow_record WHERE deleted=0 AND borrow_date BETWEEN :startDate AND :endDate GROUP BY month ORDER BY month',
 '[{"name":"startDate","label":"开始日期","type":"date"},{"name":"endDate","label":"结束日期","type":"date"}]',
 '借阅统计', 1),
('分类图书统计', '按分类统计图书数量和借阅次数',
 'SELECT bc.name as category, COUNT(b.id) as book_count, SUM(b.borrow_count) as total_borrows FROM book b LEFT JOIN book_category bc ON b.category_id = bc.id WHERE b.deleted=0 GROUP BY bc.name ORDER BY book_count DESC',
 '[]',
 '图书统计', 1),
('读者借阅排行', '读者借阅数量排行榜',
 'SELECT u.username, u.real_name, u.borrow_count FROM user u WHERE u.deleted=0 AND u.borrow_count > 0 ORDER BY u.borrow_count DESC LIMIT :topN',
 '[{"name":"topN","label":"排行人数","type":"number","default":20}]',
 '读者统计', 1);
