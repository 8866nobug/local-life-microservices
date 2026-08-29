package com.wanger.aiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 分析微服务（无数据库，仅依赖 Redis 缓存任务结果）。
 * 负责：接收报告任务、异步调 Dify 编排引擎、降级兜底、结果轮询。
 */
@SpringBootApplication(scanBasePackages = {"com.wanger.aiservice", "com.wanger.common"})
@EnableFeignClients
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
