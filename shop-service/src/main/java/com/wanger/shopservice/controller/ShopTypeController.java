package com.wanger.shopservice.controller;

import com.wanger.common.dto.Result;
import com.wanger.shopservice.entity.ShopType;
import com.wanger.shopservice.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        List<ShopType> typeList = typeService.queryTypeList();
        if(typeList==null){
            return Result.fail("查询shoptype错误");
        }
        return Result.ok(typeList);
    }
}

