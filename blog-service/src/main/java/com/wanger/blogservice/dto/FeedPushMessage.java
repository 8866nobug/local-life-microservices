package com.wanger.blogservice.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Feed 推送消息体（Producer → RocketMQ → Consumer）。
 * 发布笔记后异步把 blogId 写入作者所有粉丝的收件箱（写扩散），大V不推（发布时走拉）。
 */
@Data
public class FeedPushMessage implements Serializable {

    public static final String TOPIC = "feed-push-topic";
    public static final String CONSUMER_GROUP = "feed-push-consumer-group";

    /** 笔记 id */
    private Long blogId;

    /** 作者 id */
    private Long authorId;

    /** 发布时间戳（毫秒），作为收件箱 ZSet 的 score，用于滚动分页游标 */
    private Long score;
}
