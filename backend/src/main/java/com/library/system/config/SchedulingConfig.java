package com.library.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * 启用Spring @Scheduled 定时任务
 *
 * @author Library Team
 * @version 2.0.0
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
