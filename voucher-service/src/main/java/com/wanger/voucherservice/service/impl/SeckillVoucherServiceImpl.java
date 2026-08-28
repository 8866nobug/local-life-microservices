package com.wanger.voucherservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.common.constants.RedisConstants;
import com.wanger.common.dto.Result;
import com.wanger.common.utils.RedisIdWorker;
import com.wanger.common.utils.UserHolder;
import com.wanger.voucherservice.dto.SeckillOrderMessage;
import com.wanger.voucherservice.entity.SeckillVoucher;
import com.wanger.voucherservice.entity.VoucherOrder;
import com.wanger.voucherservice.mapper.SeckillVoucherMapper;
import com.wanger.voucherservice.service.ISeckillVoucherService;
import com.wanger.voucherservice.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    private static final String ORDER_PREFIX = "order";

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUserId();
        // 1. Lua 原子校验：库存充足 + 一人一单 + 扣减 Redis 库存
        Long theResult = stringRedisTemplate.execute(
                SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
        int result = theResult == null ? 1 : theResult.intValue();
        if (result != 0) {
            return Result.fail(result == 1 ? "库存不足" : "一人一单");
        }
        // 2. 生成订单 id，组装消息发送到 RocketMQ（traceId = orderId）
        long orderId = redisIdWorker.nextId(ORDER_PREFIX);
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setVoucherId(voucherId);
        try {
            rocketMQTemplate.syncSend(SeckillOrderMessage.TOPIC,
                    MessageBuilder.withPayload(message)
                            .setHeader(RocketMQHeaders.KEYS, String.valueOf(orderId))
                            .build());
        } catch (Exception e) {
            // 发送失败：回滚 Redis 预扣，保证用户可重试（否则会出现"扣了库存但无订单"）
            stringRedisTemplate.opsForValue().increment(RedisConstants.SECKILL_STOCK_KEY + voucherId, 1);
            stringRedisTemplate.opsForSet().remove(RedisConstants.SECKILL_ORDER_KEY + voucherId, userId.toString());
            log.error("秒杀订单消息发送失败, orderId={}", orderId, e);
            throw new RuntimeException("下单失败，请重试", e);
        }
        // 3. 立即返回订单 id
        return Result.ok(orderId);
    }

    /**
     * MQ 消费者落库：扣减数据库库存 + 创建订单，单事务。
     * 幂等：tb_voucher_order 上 (user_id, voucher_id) 唯一索引兜底，重复投递抛 DuplicateKeyException 即视为已处理。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createOrderFromMq(SeckillOrderMessage message) {
        // 1. 先创建订单（幂等：重复投递时唯一索引抛 DuplicateKeyException，直接跳过）
        VoucherOrder order = new VoucherOrder();
        order.setId(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setVoucherId(message.getVoucherId());
        try {
            voucherOrderService.save(order);
        } catch (DuplicateKeyException e) {
            log.info("订单已存在，跳过重复投递, orderId={}", message.getOrderId());
            return;
        }
        // 2. 扣减数据库库存（stock > 0 兜底防超卖）
        boolean success = lambdaUpdate()
                .eq(SeckillVoucher::getVoucherId, message.getVoucherId())
                .gt(SeckillVoucher::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        if (!success) {
            throw new RuntimeException("库存不足");
        }
    }
}
