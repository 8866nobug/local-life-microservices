package com.wanger.aiservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanger.aiservice.config.DifyProperties;
import com.wanger.aiservice.exception.DifyException;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dify 编排引擎客户端，统一封装 workflow / chat 调用、鉴权、超时、响应解析。
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
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DifyClient(DifyProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(properties.getTimeoutSeconds() * 1000);
        this.restTemplate = new RestTemplate(factory);

        // WebClient 用于读流式 SSE；connect 5s，读超时沿用 timeoutSeconds
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 阻塞执行 workflow，返回结束节点的 outputs（Map 形式）。
     *
     * @param app    应用名（见 {@link #APP_SHOP_ANALYSIS} 等常量），决定使用哪个 api-key / base-url
     * @param inputs workflow 输入变量
     */
    public Map<String, Object> runWorkflow(String app, Map<String, Object> inputs) {
        DifyProperties.App cfg = resolveApp(app);
        String url = resolveBaseUrl(cfg) + "/v1/workflows/run";

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", "ai-service");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.getApiKey());

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

    /**
     * 流式调用客服 agent（chat-messages），逐段回调增量答案。
     *
     * @param app            应用名（{@link #APP_CUSTOMER_SERVICE}）
     * @param query          用户本轮输入
     * @param inputs         附加变量（如 userId，供 Dify 私有数据节点填 X-User-Id）
     * @param conversationId 会话 id，续上下文；null 表示新会话
     * @param handler        流式回调
     */
    public void chatStream(String app, String query, Map<String, Object> inputs, String conversationId,
                           Consumer<String> onDelta, Consumer<String> onEnd) {
        DifyProperties.App cfg = resolveApp(app);
        String url = resolveBaseUrl(cfg) + "/v1/chat-messages";

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs == null ? new HashMap<String, Object>() : inputs);
        body.put("query", query);
        body.put("response_mode", "streaming");
        body.put("conversation_id", conversationId == null ? "" : conversationId);
        body.put("user", "ai-service");

        try {
            Flux<ServerSentEvent<String>> events = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                    });

            StringBuilder acc = new StringBuilder();
            events.toStream().forEach(sse -> handleEvent(sse, acc, onDelta, onEnd));
        } catch (DifyException e) {
            throw e;
        } catch (Exception e) {
            throw new DifyException("调用 Dify 客服失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析单个 SSE 事件。Dify 的 event 类型在 data JSON 的 event 字段，answer 为累计答案。
     */
    private void handleEvent(ServerSentEvent<String> sse, StringBuilder acc,
                             Consumer<String> onDelta, Consumer<String> onEnd) {
        String data = sse.data();
        if (data == null || data.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(data);
            String event = node.path("event").asText();
            if ("message".equals(event)) {
                String answer = node.path("answer").asText("");
                if (answer.length() > acc.length()) {
                    String delta = answer.substring(acc.length());
                    acc.append(delta);
                    onDelta.accept(delta);
                }
            } else if ("message_end".equals(event)) {
                onEnd.accept(node.path("conversation_id").asText(""));
            } else if ("error".equals(event)) {
                throw new DifyException("客服 agent 返回错误: " + data);
            }
        } catch (JsonProcessingException e) {
            throw new DifyException("解析 Dify SSE 失败: " + e.getMessage(), e);
        }
    }

    private DifyProperties.App resolveApp(String app) {
        DifyProperties.App cfg = properties.getApps().get(app);
        if (cfg == null) {
            throw new DifyException("未配置 Dify 应用: " + app);
        }
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new DifyException("Dify 应用 " + app + " 未配置 api-key");
        }
        return cfg;
    }

    private String resolveBaseUrl(DifyProperties.App cfg) {
        return cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()
                ? cfg.getBaseUrl() : properties.getBaseUrl();
    }

}
