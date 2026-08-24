package com.example.algorithmdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 */
@Configuration
public class AsyncConfig {

    @Bean("trackingExecutor")
    public Executor trackingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("tracking-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 线程池满时降级，不阻塞主线程
            System.err.println("埋点线程池已满，任务被拒绝");
        });
        executor.initialize();
        return executor;
    }
}