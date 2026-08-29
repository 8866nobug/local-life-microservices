package com.wanger.aiservice.feign;

import com.wanger.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 跨服务查店铺信息，用于 Dify 不可用时的降级兜底（独立于 Dify 主流程取数）。
 * 返回 Result，从 data 里取 ShopDTO（Result 无泛型，data 反序列化为 LinkedHashMap，需手动转）。
 */
@FeignClient(name = "shop-service")
public interface ShopFeignClient {

    @GetMapping("/shop/{id}")
    Result getShopById(@PathVariable("id") Long id);
}
