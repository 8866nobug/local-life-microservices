package com.wanger.voucherservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.voucherservice.entity.VoucherOrder;
import com.wanger.voucherservice.mapper.VoucherOrderMapper;
import com.wanger.voucherservice.service.IVoucherOrderService;
import org.springframework.stereotype.Service;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

}