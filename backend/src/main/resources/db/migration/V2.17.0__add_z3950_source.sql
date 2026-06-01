CREATE TABLE IF NOT EXISTS z3950_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT NOT NULL DEFAULT 210 COMMENT '端口',
    database VARCHAR(100) NOT NULL COMMENT '数据库名',
    protocol VARCHAR(10) NOT NULL DEFAULT 'SRU' COMMENT '协议: SRU/Z3950',
    charset VARCHAR(20) DEFAULT 'UTF-8' COMMENT '字符集',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    timeout INT DEFAULT 30 COMMENT '超时(秒)',
    deleted TINYINT DEFAULT 0,
    INDEX idx_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Z39.50数据源配置';

INSERT INTO z3950_source (name, host, port, database, protocol, charset, is_enabled, timeout) VALUES
('Library of Congress', 'z3950.loc.gov', 210, 'Voyager', 'SRU', 'UTF-8', 1, 30),
('中国国家图书馆', 'opac.nlc.cn', 210, 'F', 'SRU', 'UTF-8', 1, 30),
('CALIS高校图书馆', 'z3950.calis.edu.cn', 210, 'CALIS', 'SRU', 'UTF-8', 0, 30);
