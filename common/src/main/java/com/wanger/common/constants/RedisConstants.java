package com.wanger.common.constants;

public class RedisConstants {

    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String SECKILL_ORDER_KEY = "seckill:order:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    // 大V列表（全局 Set，无尾缀，member=userId；真相在 tb_follow 粉丝数，启动预热重建）
    public static final String BLOG_BIGV_KEY = "blog:bigv";
    // 笔记内容缓存（+blogId）
    public static final String BLOG_CACHE_KEY = "blog:cache:";
    public static final Long BLOG_CACHE_TTL = 30L;
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
    public static final String CACHE_SHOPTYPE_KEY = "cache:shoptype";
    public static final Long CACHE_SHOPTYPE_TTL = 30L;
    // AI 分析报告结果缓存（+taskId，一次性查看、TTL 过期自动清理）
    public static final String AI_REPORT_KEY = "ai:report:";
    public static final Long AI_REPORT_TTL = 3600L;
}
