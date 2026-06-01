CREATE TABLE IF NOT EXISTS serial_routing (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL COMMENT '订阅ID',
    issue_id BIGINT COMMENT '到刊ID',
    destination VARCHAR(100) NOT NULL COMMENT '分发目标',
    copies INT NOT NULL DEFAULT 1 COMMENT '分发份数',
    routing_order INT NOT NULL DEFAULT 1 COMMENT '分发顺序',
    routing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '分发状态: PENDING/IN_TRANSIT/DELIVERED/RETURNED',
    sent_date DATE COMMENT '发出日期',
    received_by VARCHAR(100) COMMENT '签收人',
    received_date DATE COMMENT '签收日期',
    notes TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_routing_status (routing_status),
    INDEX idx_destination (destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊路由分发表';

CREATE TABLE IF NOT EXISTS serial_routing_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL COMMENT '订阅ID',
    destination VARCHAR(100) NOT NULL COMMENT '分发目标',
    copies INT NOT NULL DEFAULT 1 COMMENT '分发份数',
    routing_order INT NOT NULL DEFAULT 1 COMMENT '分发顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期刊路由分发模板表';
