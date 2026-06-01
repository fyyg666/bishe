CREATE TABLE IF NOT EXISTS marc_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    record_type VARCHAR(10) NOT NULL DEFAULT 'BIB' COMMENT '记录类型: BIB-书目/AUTH-规范',
    leader VARCHAR(24) COMMENT 'MARC21头标区(24字节)',
    control_number VARCHAR(20) COMMENT '控制号(001字段)',
    book_id BIGINT COMMENT '关联图书ID',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/APPROVED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_record_type (record_type),
    INDEX idx_control_number (control_number),
    INDEX idx_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC21记录表';

CREATE TABLE IF NOT EXISTS marc_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字段ID',
    record_id BIGINT NOT NULL COMMENT 'MARC记录ID',
    tag VARCHAR(3) NOT NULL COMMENT '字段标签(如010/100/245)',
    indicator1 VARCHAR(1) DEFAULT ' ' COMMENT '指示符1',
    indicator2 VARCHAR(1) DEFAULT ' ' COMMENT '指示符2',
    subfields JSON COMMENT '子字段JSON: [{"code":"a","value":"..."},...]',
    display_value TEXT COMMENT '显示值(拼接后的可读文本)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_record_id (record_id),
    INDEX idx_tag (tag),
    INDEX idx_record_tag (record_id, tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC21字段表';
