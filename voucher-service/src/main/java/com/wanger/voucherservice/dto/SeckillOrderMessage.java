package com.wanger.voucherservice.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀下单消息体（Producer → RocketMQ → Consumer）。
 * orderId 由 RedisIdWorker 生成，全局唯一，既作为 RocketMQ 消息 Key，也作为幂等 traceId。
 */
@Data
public class SeckillOrderMessage implements Serializable {

    public static final String TOPIC = "seckill-order-topic";
    public static final String CONSUMER_GROUP = "seckill-order-consumer-group";

    /** 订单 id（雪花 id，幂等 traceId） */
    private Long orderId;

    /** 下单用户 id */
    private Long userId;

    /** 秒杀优惠券 id */
    private Long voucherId;
}
