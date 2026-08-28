package com.wanger.voucherservice.mq;

import com.wanger.voucherservice.dto.SeckillOrderMessage;
import com.wanger.voucherservice.service.ISeckillVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 秒杀订单消费者：单条消费，逐条落库。
 * onMessage 正常返回 = ACK（CONSUME_SUCCESS）；抛出异常 = 重投（RECONSUME_LATER），
 * 重试耗尽后进入死信队列 %DLQ%seckill-order-consumer-group。
 * 幂等靠 tb_voucher_order 唯一索引兜底；防超卖靠 Lua 预扣 + stock>0 条件更新。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = SeckillOrderMessage.TOPIC,
        consumerGroup = SeckillOrderMessage.CONSUMER_GROUP,
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadMax = 8)
public class SeckillOrderConsumer implements RocketMQListener<SeckillOrderMessage> {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public void onMessage(SeckillOrderMessage message) {
        seckillVoucherService.createOrderFromMq(message);
    }
}
