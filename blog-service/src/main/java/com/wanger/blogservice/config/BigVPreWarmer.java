package com.wanger.blogservice.config;

import com.wanger.blogservice.mapper.FollowMapper;
import com.wanger.common.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 大V列表启动预热。
 * 大V标识的持久化真相是 tb_follow 的粉丝数；Redis blog:bigv 只是缓存索引。
 * Redis 重启丢失后，这里启动时从 DB 重建，实现自愈（与 shop 的 ShopCachePreWarmer 同思路）。
 */
@Slf4j
@Component
public class BigVPreWarmer implements CommandLineRunner {

    @Resource
    private FollowMapper followMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${blog.feed.big-v-threshold:10000}")
    private long bigVThreshold;

    @Override
    public void run(String... args) {
        List<Long> bigVIds = followMapper.selectBigVIds(bigVThreshold);
        stringRedisTemplate.delete(RedisConstants.BLOG_BIGV_KEY);
        if (!bigVIds.isEmpty()) {
            stringRedisTemplate.opsForSet().add(RedisConstants.BLOG_BIGV_KEY,
                    bigVIds.stream().map(String::valueOf).toArray(String[]::new));
        }
        log.info("大V列表预热完成，共 {} 个大V", bigVIds.size());
    }
}
