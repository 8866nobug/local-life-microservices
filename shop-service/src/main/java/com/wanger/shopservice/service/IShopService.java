package com.wanger.shopservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanger.common.dto.Result;
import com.wanger.shopservice.entity.Shop;

import java.util.List;

public interface IShopService extends IService<Shop> {

    Shop queryById(Long id);

    Result updateShop(Shop shop);

    List<Shop> queryShopByType(Integer typeId, Integer current);

    List<Shop> queryShopByName(String name, Integer current);
}
