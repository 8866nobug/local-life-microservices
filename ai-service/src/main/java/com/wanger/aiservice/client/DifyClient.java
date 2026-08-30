package com.wanger.aiservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanger.aiservice.config.DifyProperties;
import com.wanger.aiservice.exception.DifyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify 编排引擎客户端，统一封装 workflow 调用、鉴权、超时、响应解析。
 * 业务侧只依赖本类，不散落 HTTP 调用；失败统一抛 {@link DifyException} 供降级捕捉。
 */
@Slf4j
@Component
public class DifyClient {

    /**
     * Dify 应用名：店铺数据分析报告（店铺 + 关联优惠券）。
     */
    public static final String APP_SHOP_ANALYSIS = "shop-analysis";

    /**
     * Dify 应用名：智能客服 agent（多轮对话）。
     */
    public static final String APP_CUSTOMER_SERVICE = "customer-service";

    private final DifyProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DifyClient(DifyProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(properties.getTimeoutSeconds() * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 阻塞执行 workflow，返回结束节点的 outputs（Map 形式）。
     *
     * @param app    应用名（见 {@link #APP_SHOP_ANALYSIS} 等常量），决定使用哪个 api-key / base-url
     * @param inputs workflow 输入变量
     */
    public Map<String, Object> runWorkflow(String app, Map<String, Object> inputs) {
        DifyProperties.App cfg = properties.getApps().get(app);
        if (cfg == null) {
            throw new DifyException("未配置 Dify 应用: " + app);
        }
        String apiKey = cfg.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new DifyException("Dify 应用 " + app + " 未配置 api-key");
        }
        String baseUrl = cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()
                ? cfg.getBaseUrl() : properties.getBaseUrl();
        String url = baseUrl + "/v1/workflows/run";

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", "ai-service");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            String status = data.path("status").asText();
            if (!"succeeded".equals(status)) {
                String error = data.path("error").asText("");
                throw new DifyException("workflow 执行失败，status=" + status + ", error=" + error);
            }
            JsonNode outputs = data.path("outputs");
            if (outputs.isMissingNode() || outputs.isNull()) {
                throw new DifyException("workflow 未返回 outputs");
            }
            return objectMapper.convertValue(outputs, new TypeReference<Map<String, Object>>() {
            });
        } catch (DifyException e) {
            throw e;
        } catch (Exception e) {
            throw new DifyException("调用 Dify 失败: " + e.getMessage(), e);
        }
    }
}
