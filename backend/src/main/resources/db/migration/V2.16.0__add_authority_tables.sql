CREATE TABLE IF NOT EXISTS authority_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规范记录ID',
    authority_type VARCHAR(20) NOT NULL COMMENT '类型: PERSONAL-个人名称/CORPORATE-团体名称/TOPIC-主题',
    heading VARCHAR(255) NOT NULL COMMENT '规范标目(统一形式)',
    variant_headings JSON COMMENT '变异形式列表',
    source VARCHAR(50) COMMENT '来源(如LC/NLC)',
    source_id VARCHAR(50) COMMENT '来源ID',
    note TEXT COMMENT '附注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_type_heading (authority_type, heading),
    INDEX idx_source (source, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规范记录表';
