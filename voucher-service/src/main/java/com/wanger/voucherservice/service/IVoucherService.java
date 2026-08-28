package com.wanger.voucherservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanger.common.dto.Result;
import com.wanger.voucherservice.entity.Voucher;

public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
