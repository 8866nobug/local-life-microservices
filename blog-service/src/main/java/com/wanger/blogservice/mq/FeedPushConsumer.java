package com.wanger.blogservice.mq;

import com.wanger.blogservice.dto.FeedPushMessage;
import com.wanger.blogservice.service.IBlogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Feed 推送消费者：把作者新发布的笔记异步写入其粉丝的收件箱（写扩散）。
 * 大V在发布时不推（走拉），所以到这里消费的都是普通用户的消息，粉丝量有限、写放大可控。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = FeedPushMessage.TOPIC,
        consumerGroup = FeedPushMessage.CONSUMER_GROUP,
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadMax = 8)
public class FeedPushConsumer implements RocketMQListener<FeedPushMessage> {

    @Resource
    private IBlogService blogService;

    @Override
    public void onMessage(FeedPushMessage message) {
        blogService.pushToInbox(message);
    }
}
