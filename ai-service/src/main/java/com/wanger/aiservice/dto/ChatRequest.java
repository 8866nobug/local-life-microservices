package com.wanger.aiservice.dto;

import lombok.Data;

/**
 * 客服对话请求体。仅含用户输入 query；
 * 用户身份来自网关透传的 X-User-Id（经 UserInterceptor 存入 UserHolder），禁止前端传 userId。
 */
@Data
public class ChatRequest {
    private String query;
}
