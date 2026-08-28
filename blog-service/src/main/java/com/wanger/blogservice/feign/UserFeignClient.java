package com.wanger.blogservice.feign;

import com.wanger.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 跨服务查用户信息。
 * 返回 Result，blog 侧从 data 里取 UserDTO（Result 无泛型，data 反序列化为 LinkedHashMap，需手动转）。
 */
@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/user/{id}")
    Result getUserById(@PathVariable("id") Long id);
}
