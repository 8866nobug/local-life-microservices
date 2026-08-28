package com.wanger.voucherservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanger.common.dto.Result;
import com.wanger.voucherservice.dto.SeckillOrderMessage;
import com.wanger.voucherservice.entity.SeckillVoucher;

public interface ISeckillVoucherService extends IService<SeckillVoucher> {

    Result seckillVoucher(Long voucherId);

    void createOrderFromMq(SeckillOrderMessage message);
}
