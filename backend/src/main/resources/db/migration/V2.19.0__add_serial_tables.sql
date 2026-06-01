CREATE TABLE IF NOT EXISTS serial_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '刊名',
    issn VARCHAR(20) COMMENT 'ISSN',
    vendor_id BIGINT COMMENT '供应商ID',
    fund_id BIGINT COMMENT '预算ID',
    start_date DATE COMMENT '订阅开始日期',
    end_date DATE COMMENT '订阅结束日期',
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' COMMENT '频率: DAILY/WEEKLY/BIWEEKLY/MONTHLY/QUARTERLY/YEARLY',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/TERMINATED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_issn (issn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊订阅表';

CREATE TABLE IF NOT EXISTS serial_issue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL COMMENT '订阅ID',
    volume VARCHAR(20) COMMENT '卷',
    issue VARCHAR(20) COMMENT '期',
    expected_date DATE COMMENT '预期到刊日期',
    received_date DATE COMMENT '实际到刊日期',
    status VARCHAR(20) NOT NULL DEFAULT 'EXPECTED' COMMENT '状态: EXPECTED/RECEIVED/MISSING',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_status (status),
    INDEX idx_expected_date (expected_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊到刊表';
