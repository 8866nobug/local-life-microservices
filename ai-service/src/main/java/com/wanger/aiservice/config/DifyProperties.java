package com.wanger.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dify 编排引擎配置，从 Nacos（ai-service.yaml 的 dify.*）读取。
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    /**
     * Dify API 根地址（自托管默认 http://localhost:5001，Cloud 为 https://api.dify.ai）
     */
    private String baseUrl;

    /**
     * 应用 API Key（Dify 应用「访问 API」页生成）
     */
    private String apiKey;

    /**
     * workflow 阻塞调用读超时（秒）
     */
    private Integer timeoutSeconds = 60;
}
