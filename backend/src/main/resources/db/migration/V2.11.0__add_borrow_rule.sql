CREATE TABLE IF NOT EXISTS borrow_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    reader_type VARCHAR(20) NOT NULL COMMENT '读者类型: READER/LIBRARIAN/ADMIN',
    book_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '图书类型: NORMAL/REFERENCE',
    max_borrow INT NOT NULL DEFAULT 5 COMMENT '最大借阅数量',
    max_days INT NOT NULL DEFAULT 30 COMMENT '最大借阅天数',
    max_renew INT NOT NULL DEFAULT 1 COMMENT '最大续借次数',
    renew_days INT NOT NULL DEFAULT 15 COMMENT '每次续借天数',
    fine_per_day DECIMAL(10,2) NOT NULL DEFAULT 0.10 COMMENT '每日罚款金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE INDEX idx_reader_book_type (reader_type, book_type),
    INDEX idx_reader_type (reader_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅规则表';

INSERT INTO borrow_rule (reader_type, book_type, max_borrow, max_days, max_renew, renew_days, fine_per_day) VALUES
('READER', 'NORMAL', 5, 30, 1, 15, 0.10),
('READER', 'REFERENCE', 2, 14, 0, 0, 0.10),
('LIBRARIAN', 'NORMAL', 10, 60, 2, 15, 0.10),
('LIBRARIAN', 'REFERENCE', 5, 30, 1, 15, 0.10),
('ADMIN', 'NORMAL', 10, 60, 2, 15, 0.10),
('ADMIN', 'REFERENCE', 5, 30, 1, 15, 0.10);
