package com.wanger.voucherservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wanger.voucherservice.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}