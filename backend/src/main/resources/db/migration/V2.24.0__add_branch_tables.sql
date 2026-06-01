CREATE TABLE IF NOT EXISTS `branch` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '分馆名称',
    `code` VARCHAR(50) NOT NULL COMMENT '分馆编码',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `opening_hours` VARCHAR(255) DEFAULT NULL COMMENT '开放时间描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用, 0=停用',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上级分馆ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status` (`status`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分馆表';

INSERT INTO `branch` (`id`, `name`, `code`, `address`, `status`) VALUES
(1, '总馆', 'MAIN', '图书馆总馆', 1),
(2, '东区分馆', 'EAST', '东区分馆', 1),
(3, '西区分馆', 'WEST', '西区分馆', 1);

ALTER TABLE `book` ADD COLUMN `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID' AFTER `id`;
ALTER TABLE `borrow_record` ADD COLUMN `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID' AFTER `id`;
ALTER TABLE `seat` ADD COLUMN `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID' AFTER `id`;
ALTER TABLE `reading_room` ADD COLUMN `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID' AFTER `id`;
ALTER TABLE `serial_subscription` ADD COLUMN `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID' AFTER `id`;
ALTER TABLE `digital_resource` ADD COLUMN `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID' AFTER `id`;
ALTER TABLE `purchase_order` ADD COLUMN `branch_id` BIGINT DEFAULT NULL COMMENT '分馆ID' AFTER `id`;

ALTER TABLE `book` ADD KEY `idx_branch_id` (`branch_id`);
ALTER TABLE `borrow_record` ADD KEY `idx_branch_id` (`branch_id`);
ALTER TABLE `seat` ADD KEY `idx_branch_id` (`branch_id`);
ALTER TABLE `reading_room` ADD KEY `idx_branch_id` (`branch_id`);
ALTER TABLE `serial_subscription` ADD KEY `idx_branch_id` (`branch_id`);
ALTER TABLE `digital_resource` ADD KEY `idx_branch_id` (`branch_id`);
ALTER TABLE `purchase_order` ADD KEY `idx_branch_id` (`branch_id`);

UPDATE `book` SET `branch_id` = 1 WHERE `branch_id` IS NULL;
UPDATE `borrow_record` SET `branch_id` = 1 WHERE `branch_id` IS NULL;
UPDATE `seat` SET `branch_id` = 1 WHERE `branch_id` IS NULL;
UPDATE `reading_room` SET `branch_id` = 1 WHERE `branch_id` IS NULL;
UPDATE `serial_subscription` SET `branch_id` = 1 WHERE `branch_id` IS NULL;
UPDATE `digital_resource` SET `branch_id` = 1 WHERE `branch_id` IS NULL;
UPDATE `purchase_order` SET `branch_id` = 1 WHERE `branch_id` IS NULL;
