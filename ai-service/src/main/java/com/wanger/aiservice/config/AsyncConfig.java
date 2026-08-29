package com.wanger.aiservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 报告生成的异步线程池。
 * 调 Dify 是网络 IO 密集：核心线程少、峰值线程可扩展、队列缓冲、CallerRunsPolicy 背压。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("aiReportExecutor")
    public ThreadPoolTaskExecutor aiReportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-report-");
        // 队列满时由调用线程（Tomcat 线程）自行执行，实现背压，避免静默丢任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
