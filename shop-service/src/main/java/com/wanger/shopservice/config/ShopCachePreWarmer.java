package com.wanger.shopservice.config;

import com.wanger.common.constants.RedisConstants;
import com.wanger.common.redis.CacheClient;
import com.wanger.shopservice.entity.Shop;
import com.wanger.shopservice.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 启动预热：把 tb_shop 全量数据以「逻辑过期」结构写入 Redis。
 * queryById 采用 CacheClient#getWithBreakDown（逻辑过期 + 异步重建），依赖此处预热的缓存，
 * 未预热的 key 会直接返回 null，因此必须在服务启动时执行。
 */
@Slf4j
@Component
public class ShopCachePreWarmer implements CommandLineRunner {

    @Autowired
    private IShopService shopService;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public void run(String... args) {
        log.info("开始预热商铺缓存...");
        List<Shop> shops = shopService.list();
        shops.forEach(shop -> cacheClient.setWithLogicExpireTime(
                RedisConstants.CACHE_SHOP_KEY + shop.getId(),
                shop,
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES));
        log.info("商铺缓存预热完成，共 {} 条", shops.size());
    }
}
