package com.wanger.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dify 编排引擎配置，从 Nacos（ai-service.yaml 的 dify.*）读取。
 * 一个 Dify 应用一个 API Key，多应用通过 {@link #apps} 按应用名区分；
 * {@link #baseUrl} 为全局默认地址，单个应用可用自身的 baseUrl 覆盖（跨部署场景）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    /**
     * Dify API 根地址（全局默认；自托管默认 http://localhost:5001，Cloud 为 https://api.dify.ai）
     */
    private String baseUrl;

    /**
     * 各 Dify 应用配置，key 为应用名（如 shop-analysis / customer-service）。
     */
    private Map<String, App> apps;

    /**
     * workflow 阻塞调用读超时（秒）
     */
    private Integer timeoutSeconds = 60;

    /**
     * 单个 Dify 应用配置。
     */
    @Data
    public static class App {

        /**
         * 应用 API Key（Dify 应用「访问 API」页生成）
         */
        private String apiKey;

        /**
         * 该应用的 Dify API 根地址，为空则继承全局 {@link DifyProperties#baseUrl}。
         */
        private String baseUrl;
    }
}
