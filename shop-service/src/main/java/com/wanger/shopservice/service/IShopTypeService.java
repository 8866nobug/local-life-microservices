package com.wanger.shopservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanger.shopservice.entity.ShopType;

import java.util.List;

public interface IShopTypeService extends IService<ShopType> {

    List<ShopType> queryTypeList();
}
