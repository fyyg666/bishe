-- ============================================================
-- V2.10.0: 创建 notification 表
-- 用于用户通知功能（逾期提醒、到期提醒、预约提醒等）
-- ============================================================

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type VARCHAR(50) DEFAULT 'SYSTEM' COMMENT '通知类型：SYSTEM-系统通知，BORROW-借阅通知，SEAT-座位通知，CREDIT-积分通知',
    status VARCHAR(20) DEFAULT 'UNREAD' COMMENT '通知状态：UNREAD-未读，READ-已读',
    read_at DATETIME DEFAULT NULL COMMENT '阅读时间',
    biz_id BIGINT DEFAULT NULL COMMENT '关联业务ID（借阅记录ID/预约ID等）',
    announcement_id BIGINT DEFAULT NULL COMMENT '关联公告ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_type (user_id, type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知表';
