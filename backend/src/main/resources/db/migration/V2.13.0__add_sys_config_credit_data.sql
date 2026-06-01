INSERT INTO sys_config (config_key, config_value, description) VALUES
('credit.borrow_reward', '5', '借阅奖励积分'),
('credit.return_on_time', '1', '按时归还奖励积分'),
('credit.return_early', '2', '提前归还奖励积分'),
('credit.overdue_per_day', '5', '逾期每天扣除积分'),
('credit.no_show', '2', '预约未签到扣除积分'),
('credit.damage_penalty', '50', '图书损坏扣除积分'),
('credit.lost_penalty', '100', '图书丢失扣除积分'),
('credit.volunteer_per_hour', '10', '每小时志愿服务奖励积分'),
('credit.checkin_reward', '1', '座位签到奖励积分'),
('seat.cancel_before_hours', '2', '座位预约取消提前小时数'),
('library.default_password', '123456', '读者默认密码')
ON DUPLICATE KEY UPDATE config_value = config_value;
