package com.wanger.voucherservice.controller;

import com.wanger.common.dto.Result;
import com.wanger.voucherservice.service.ISeckillVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Autowired
    ISeckillVoucherService seckillVoucherService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        return  seckillVoucherService.seckillVoucher(voucherId);
    }
}