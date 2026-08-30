package com.wanger.aiservice.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 智能客服对话服务。
 */
public interface ChatService {

    /**
     * 发起一轮对话，返回 SSE 流式响应。
     *
     * @param userId 当前用户（来自网关透传的 X-User-Id）
     * @param query  用户输入
     */
    SseEmitter chat(Long userId, String query);

    /**
     * 清空当前用户会话（conversation_id 与历史消息）。
     */
    void reset(Long userId);
}
