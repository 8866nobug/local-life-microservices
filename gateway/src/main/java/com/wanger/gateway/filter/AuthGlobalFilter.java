package com.wanger.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanger.common.constants.HeaderConstants;
import com.wanger.common.constants.RedisConstants;
import com.wanger.common.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统一鉴权过滤器。
 * 负责：白名单放行、token 校验、滑动续期、透传 X-User-Id。
 * 业务服务不再重复做 token 鉴权，只信任 X-User-Id。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 无需登录即可访问的路径，逗号分隔，支持 Ant 风格（如 /shop/**）
     */
    @Value("${auth.whitelist:}")
    private String whitelist;

    public AuthGlobalFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单直接放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 2. 取 token
        String token = exchange.getRequest().getHeaders().getFirst("authorization");
        if (token == null || token.isBlank()) {
            return unauthorized(exchange);
        }

        // 3. 查 Redis 校验 token 并拿到 userId
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if (userMap.isEmpty()) {
            return unauthorized(exchange);
        }
        Object id = userMap.get("id");
        if (id == null) {
            return unauthorized(exchange);
        }

        // 4. 滑动续期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        // 5. 透传身份
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(HeaderConstants.USER_ID, id.toString())
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhitelisted(String path) {
        if (whitelist == null || whitelist.isBlank()) {
            return false;
        }
        for (String pattern : whitelist.split(",")) {
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(Result.fail("未登录"));
        } catch (Exception e) {
            bytes = "{\"success\":false,\"errorMsg\":\"未登录\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 保证在路由转发之前执行
        return -1;
    }
}
