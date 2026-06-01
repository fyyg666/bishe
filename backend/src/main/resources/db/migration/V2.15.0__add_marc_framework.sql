CREATE TABLE IF NOT EXISTS marc_framework (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '框架ID',
    name VARCHAR(100) NOT NULL COMMENT '框架名称',
    code VARCHAR(20) NOT NULL COMMENT '框架代码',
    record_type VARCHAR(10) NOT NULL DEFAULT 'BIB' COMMENT '适用记录类型',
    description VARCHAR(200) COMMENT '描述',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认框架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC编目框架表';

CREATE TABLE IF NOT EXISTS marc_framework_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    framework_id BIGINT NOT NULL COMMENT '框架ID',
    tag VARCHAR(3) NOT NULL COMMENT '字段标签',
    indicator1 VARCHAR(1) DEFAULT ' ' COMMENT '默认指示符1',
    indicator2 VARCHAR(1) DEFAULT ' ' COMMENT '默认指示符2',
    required TINYINT DEFAULT 0 COMMENT '是否必填',
    repeatable TINYINT DEFAULT 1 COMMENT '是否可重复',
    default_subfields JSON COMMENT '默认子字段',
    sort_order INT DEFAULT 0 COMMENT '排序',
    INDEX idx_framework_id (framework_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MARC框架字段配置表';

INSERT INTO marc_framework (name, code, record_type, description, is_default) VALUES
('图书', 'BOOK', 'BIB', '普通图书编目框架', 1),
('连续出版物', 'SERIAL', 'BIB', '期刊/报纸编目框架', 0),
('地图', 'MAP', 'BIB', '地图资料编目框架', 0),
('音像', 'AV', 'BIB', '音像资料编目框架', 0);

INSERT INTO marc_framework_field (framework_id, tag, indicator1, indicator2, required, repeatable, default_subfields, sort_order) VALUES
(1, '001', ' ', ' ', 1, 0, NULL, 1),
(1, '010', ' ', ' ', 1, 0, '[{"code":"a","value":""},{"code":"d","value":""}]', 2),
(1, '100', '1', ' ', 1, 0, '[{"code":"a","value":""}]', 3),
(1, '245', '1', '0', 1, 0, '[{"code":"a","value":""},{"code":"b","value":""},{"code":"c","value":""}]', 4),
(1, '250', ' ', ' ', 0, 0, '[{"code":"a","value":""}]', 5),
(1, '260', ' ', ' ', 1, 0, '[{"code":"a","value":""},{"code":"b","value":""},{"code":"c","value":""}]', 6),
(1, '300', ' ', ' ', 0, 0, '[{"code":"a","value":""},{"code":"b","value":""},{"code":"c","value":""}]', 7),
(1, '650', ' ', '0', 0, 1, '[{"code":"a","value":""}]', 8),
(1, '905', ' ', ' ', 0, 0, '[{"code":"a","value":""}]', 9);
