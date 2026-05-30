package com.library.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 为系统中各类异步操作提供统一的线程池。
 * 采用 ThreadPoolTaskExecutor，核心线程数根据 CPU 核心数动态调整，
 * 配置合理的队列容量和拒绝策略，避免任务丢失。
 * 实现 AsyncConfigurer 接口以处理 @Async 方法的未捕获异常。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /** 通用异步线程池Bean名称 */
    public static final String ASYNC_EXECUTOR = "asyncExecutor";

    /**
     * 通用异步任务线程池
     * <p>
     * 用于处理系统中的各类异步任务（如通知发送、数据同步等），
     * 与业务请求线程隔离，避免长时间任务阻塞接口响应。
     * </p>
     *
     * @return 异步任务线程池执行器
     */
    @Bean(ASYNC_EXECUTOR)
    @Override
    public Executor getAsyncExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(2, cpuCores * 2));
        executor.setMaxPoolSize(Math.max(4, cpuCores * 4));
        executor.setQueueCapacity(256);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("通用异步线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity=256",
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }

    /**
     * @Async 方法未捕获异常处理器
     * <p>
     * 日志记录异常信息，避免异步任务异常静默丢失。
     * </p>
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("异步任务执行异常: method={}.{}, error={}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        ex.getMessage(), ex);
            }
        };
    }
}
