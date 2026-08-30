package com.wanger.aiservice.controller;

import com.wanger.aiservice.dto.ChatRequest;
import com.wanger.aiservice.service.ChatService;
import com.wanger.common.dto.Result;
import com.wanger.common.utils.UserHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

/**
 * 智能客服接口（需登录，走网关鉴权，UserInterceptor 已从 X-User-Id 取出 userId 存入 UserHolder）。
 * POST /ai/customer_chat 流式对话；POST /ai/customer_chat/reset 清空会话。
 */
@RestController
@RequestMapping("/ai/customer_chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        Long userId = UserHolder.getUserId();
        return chatService.chat(userId, request.getQuery());
    }

    @PostMapping("/reset")
    public Result reset() {
        Long userId = UserHolder.getUserId();
        chatService.reset(userId);
        return Result.ok();
    }
}
