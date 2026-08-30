package com.wanger.aiservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanger.aiservice.client.DifyClient;
import com.wanger.aiservice.service.ChatService;
import com.wanger.common.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 客服对话实现：Redis 存会话（conversation_id + 消息历史），Dify 流式结果经 SseEmitter 转发给前端。
 * 用户身份 userId 来自网关透传（UserHolder），作为 Dify 的 input 传给客服 agent。
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private DifyClient difyClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource(name = "aiReportExecutor")
    private ThreadPoolTaskExecutor executor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SseEmitter chat(Long userId, String query) {
        SseEmitter emitter = new SseEmitter(120_000L);
        String convKey = RedisConstants.AI_CHAT_CONV_KEY + userId;
        String historyKey = RedisConstants.AI_CHAT_HISTORY_KEY + userId;
        String conversationId = stringRedisTemplate.opsForValue().get(convKey);

        // 先记录用户问题，回答在流结束后记录
        saveMessage(historyKey, "user", query);

        // 异步执行流式转发，controller 立即返回 emitter，由本线程逐步 send
        executor.execute(() -> {
            StringBuilder fullAnswer = new StringBuilder();
            try {
                Map<String, Object> inputs = Map.of("userId", String.valueOf(userId));
                difyClient.chatStream(DifyClient.APP_CUSTOMER_SERVICE, query, inputs, conversationId,
                        delta -> {
                            fullAnswer.append(delta);
                            send(emitter, delta);
                        },
                        convId -> {
                            try {
                                stringRedisTemplate.opsForValue().set(convKey, convId,
                                        RedisConstants.AI_CHAT_CONV_TTL, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                log.warn("保存 conversation_id 失败，userId={}", userId, e);
                            }
                            saveMessage(historyKey, "assistant", fullAnswer.toString());
                            emitter.complete();
                        });
            } catch (Exception e) {
                log.warn("客服 agent 调用失败，userId={}", userId, e);
                send(emitter, "客服暂时不可用，请稍后重试。");
                emitter.complete();
            }
        });

        return emitter;
    }

    @Override
    public void reset(Long userId) {
        stringRedisTemplate.delete(RedisConstants.AI_CHAT_CONV_KEY + userId);
        stringRedisTemplate.delete(RedisConstants.AI_CHAT_HISTORY_KEY + userId);
    }

    private void send(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException | IllegalStateException e) {
            // 客户端断开或连接已关闭，忽略
            log.debug("SSE 发送失败", e);
        }
    }

    private void saveMessage(String historyKey, String role, String content) {
        try {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", role);
            msg.put("content", content);
            stringRedisTemplate.opsForList().leftPush(historyKey, objectMapper.writeValueAsString(msg));
            // 只保留最近 50 条
            stringRedisTemplate.opsForList().trim(historyKey, 0, 49);
            stringRedisTemplate.expire(historyKey, RedisConstants.AI_CHAT_HISTORY_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("写入会话历史失败，key={}", historyKey, e);
        }
    }
}
