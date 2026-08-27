package com.wanger.shopservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.common.constants.RedisConstants;
import com.wanger.common.constants.SystemConstants;
import com.wanger.common.dto.Result;
import com.wanger.common.redis.CacheClient;
import com.wanger.shopservice.entity.Shop;
import com.wanger.shopservice.mapper.ShopMapper;
import com.wanger.shopservice.service.IShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Shop queryById(Long id) {
        // 逻辑过期：缓存未命中返回 null（依赖启动预热写入）；命中但逻辑过期则互斥锁 + 异步重建
        return cacheClient.getWithBreakDown(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺id==null");
        }
        // 更新数据库
        updateById(shop);
        // 删除对应缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    @Override
    public List<Shop> queryShopByType(Integer typeId, Integer current) {
        Page<Shop> page = query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        return page.getRecords();
    }

    @Override
    public List<Shop> queryShopByName(String name, Integer current) {
        Page<Shop> page = query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        return page.getRecords();
    }
}
