package com.wanger.shopservice.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.common.constants.RedisConstants;
import com.wanger.shopservice.entity.ShopType;
import com.wanger.shopservice.mapper.ShopTypeMapper;
import com.wanger.shopservice.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList() {
        String key = RedisConstants.CACHE_SHOPTYPE_KEY;
        // 查缓存
        List<String> shopTypes = stringRedisTemplate.opsForList().range(key, 0, -1);
        // 命中，返回
        if (shopTypes != null && !shopTypes.isEmpty()) {
            return shopTypes.stream()
                    .map(s -> JSONUtil.toBean(s, ShopType.class))
                    .collect(Collectors.toList());
        }
        // 未命中，查数据库
        List<ShopType> shopTypeList = lambdaQuery().orderByAsc(ShopType::getId).list();
        // 空库直接返回，不写空缓存
        if (shopTypeList == null || shopTypeList.isEmpty()) {
            return shopTypeList;
        }
        // 重建缓存并设置过期时间，避免列表永不过期
        stringRedisTemplate.delete(key);
        shopTypeList.forEach(shopType -> stringRedisTemplate.opsForList().rightPush(key, JSONUtil.toJsonStr(shopType)));
        stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);
        return shopTypeList;
    }
}
