package com.library.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 为系统中各类异步操作（如操作日志异步保存、邮件异步发送等）提供统一的线程池。
 * 采用 ThreadPoolTaskExecutor，核心线程数根据 CPU 核心数动态调整，
 * 配置合理的队列容量和拒绝策略，避免任务丢失。
 * </p>
 *
 * <p>线程池参数说明：</p>
 * <ul>
 *   <li>核心线程数：CPU核心数 × 2，保证基础并发能力</li>
 *   <li>最大线程数：CPU核心数 × 4，应对突发流量</li>
 *   <li>队列容量：256，缓冲短时任务堆积</li>
 *   <li>拒绝策略：CallerRunsPolicy，队列满时由调用线程执行，避免任务丢失</li>
 *   <li>空闲存活时间：60秒，空闲线程自动回收</li>
 * </ul>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 操作日志专用异步线程池Bean名称 */
    public static final String OPERATION_LOG_EXECUTOR = "operationLogExecutor";

    /**
     * 操作日志异步保存线程池
     * <p>
     * 专用于 OperationLogAspect 的日志异步写入，与业务线程池隔离，
     * 避免日志写入影响业务接口响应时间。
     * </p>
     *
     * @return 操作日志线程池执行器
     */
    @Bean(OPERATION_LOG_EXECUTOR)
    public Executor operationLogExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(2, cpuCores * 2));
        executor.setMaxPoolSize(Math.max(4, cpuCores * 4));
        executor.setQueueCapacity(256);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("op-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("操作日志异步线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity=256",
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }
}
