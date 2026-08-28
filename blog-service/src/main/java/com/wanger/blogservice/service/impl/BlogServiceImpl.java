package com.wanger.blogservice.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.blogservice.dto.FeedPushMessage;
import com.wanger.blogservice.entity.Blog;
import com.wanger.blogservice.entity.Follow;
import com.wanger.blogservice.feign.UserFeignClient;
import com.wanger.blogservice.mapper.BlogMapper;
import com.wanger.blogservice.service.IBlogService;
import com.wanger.blogservice.service.IFollowService;
import com.wanger.common.constants.RedisConstants;
import com.wanger.common.dto.Result;
import com.wanger.common.dto.UserDTO;
import com.wanger.common.redis.CacheClient;
import com.wanger.common.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private CacheClient cacheClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private UserFeignClient userFeignClient;

    /** 大V判定阈值：粉丝数超过该值走「拉」，否则走「推」 */
    @Value("${blog.feed.big-v-threshold:10000}")
    private long bigVThreshold;

    @Override
    public Result saveBlog(Blog blog) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        blog.setUserId(userId);
        // 显式设置创建时间（DB 默认值不会回填到实体），用于计算收件箱 score
        LocalDateTime now = LocalDateTime.now();
        blog.setCreateTime(now);
        save(blog);

        // 推拉分流：大V走拉（不推，仅入大V列表），普通用户走推（发 MQ 异步写扩散）
        long fansCount = followService.count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowUserId, userId));
        if (fansCount > bigVThreshold) {
            stringRedisTemplate.opsForSet().add(RedisConstants.BLOG_BIGV_KEY, userId.toString());
        } else {
            FeedPushMessage message = new FeedPushMessage();
            message.setBlogId(blog.getId());
            message.setAuthorId(userId);
            message.setScore(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            rocketMQTemplate.syncSend(FeedPushMessage.TOPIC,
                    MessageBuilder.withPayload(message)
                            .setHeader(RocketMQHeaders.KEYS, String.valueOf(blog.getId()))
                            .build());
        }
        return Result.ok(blog.getId());
    }

    @Override
    public Result deleteBlog(Long id) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        if (!blog.getUserId().equals(userId)) {
            return Result.fail("只能删除自己的笔记");
        }
        removeById(id);
        // 失效笔记内容缓存
        stringRedisTemplate.delete(RedisConstants.BLOG_CACHE_KEY + id);
        return Result.ok();
    }

    @Override
    public Blog queryBlogById(Long id) {
        return cacheClient.getWithPassThrough(
                RedisConstants.BLOG_CACHE_KEY, id, Blog.class,
                this::getById, RedisConstants.BLOG_CACHE_TTL, TimeUnit.MINUTES);
    }

    @Override
    public void pushToInbox(FeedPushMessage message) {
        // 查作者所有粉丝，逐一写入其收件箱（写扩散；普通用户粉丝有限）
        List<Follow> fans = followService.list(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowUserId, message.getAuthorId()));
        if (fans.isEmpty()) {
            return;
        }
        for (Follow fan : fans) {
            stringRedisTemplate.opsForZSet().add(
                    RedisConstants.FEED_KEY + fan.getUserId(),
                    message.getBlogId().toString(),
                    message.getScore());
        }
    }

    @Override
    public Result queryBlogOfFollow(Long lastTime, Integer pageSize) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        if (lastTime == null) {
            lastTime = System.currentTimeMillis();
        }

        // 1. 我关注的人 id 集合（读时兜底 + 筛大V都用它）
        List<Long> followUserIds = followService.list(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, userId))
                .stream().map(Follow::getFollowUserId).collect(Collectors.toList());
        Set<Long> followIdSet = new HashSet<>(followUserIds);

        // 2. 推的部分：读收件箱（score < lastTime 的 pageSize 条，倒序）
        Set<String> inboxIds = stringRedisTemplate.opsForZSet()
                .reverseRangeByScore(RedisConstants.FEED_KEY + userId, 0, lastTime - 1, 0, pageSize);

        // 3. 拉的部分：我关注的人里的大V，实时查其最近笔记（同样按 lastTime 滚动）
        List<Long> bigVIds = followUserIds.stream()
                .filter(uid -> Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                        .isMember(RedisConstants.BLOG_BIGV_KEY, uid.toString())))
                .collect(Collectors.toList());
        List<Blog> bigVBlogs = Collections.emptyList();
        if (!bigVIds.isEmpty()) {
            LocalDateTime last = Instant.ofEpochMilli(lastTime)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            bigVBlogs = list(new LambdaQueryWrapper<Blog>()
                    .in(Blog::getUserId, bigVIds)
                    .lt(Blog::getCreateTime, last)
                    .orderByDesc(Blog::getCreateTime)
                    .last("limit " + pageSize));
        }

        // 4. 合并：去重 + 读时兜底（取关残留过滤）+ 按时间倒序 + 截取一页
        Map<Long, Blog> merged = new LinkedHashMap<>();
        for (String id : inboxIds) {
            Blog blog = queryBlogById(Long.valueOf(id));
            if (blog != null && followIdSet.contains(blog.getUserId())) {
                merged.put(blog.getId(), blog);
            }
        }
        for (Blog blog : bigVBlogs) {
            merged.putIfAbsent(blog.getId(), blog);
        }
        List<Blog> blogs = new ArrayList<>(merged.values());
        blogs.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        if (blogs.size() > pageSize) {
            blogs = blogs.subList(0, pageSize);
        }

        // 5. 填充作者信息（Feign 查 UserDTO，同请求内去重）、点赞状态、游标 score
        Map<Long, UserDTO> userCache = new HashMap<>();
        for (Blog blog : blogs) {
            fillAuthor(blog, userCache);
            blog.setIsLike(Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                    .isMember(RedisConstants.BLOG_LIKED_KEY + blog.getId(), userId.toString())));
            blog.setScore(blog.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        return Result.ok(blogs);
    }

    /**
     * 填充笔记作者昵称/头像；同一请求内按 userId 去重，避免重复 Feign。
     */
    private void fillAuthor(Blog blog, Map<Long, UserDTO> userCache) {
        Long authorId = blog.getUserId();
        UserDTO dto = userCache.get(authorId);
        if (dto == null) {
            try {
                Result r = userFeignClient.getUserById(authorId);
                if (r != null && Boolean.TRUE.equals(r.getSuccess()) && r.getData() != null) {
                    dto = JSONUtil.toBean(JSONUtil.toJsonStr(r.getData()), UserDTO.class);
                    userCache.put(authorId, dto);
                }
            } catch (Exception e) {
                log.warn("查用户信息失败, userId={}", authorId, e);
            }
        }
        if (dto != null) {
            blog.setName(dto.getNickName());
            blog.setIcon(dto.getIcon());
        }
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            // 已点赞 → 取消
            stringRedisTemplate.opsForSet().remove(key, userId.toString());
        } else {
            // 未点赞 → 点赞
            stringRedisTemplate.opsForSet().add(key, userId.toString());
        }
        Long likeCount = stringRedisTemplate.opsForSet().size(key);
        Map<String, Object> data = new HashMap<>();
        data.put("isLike", !Boolean.TRUE.equals(isMember));
        data.put("likeCount", likeCount);
        // TODO 点赞数异步落库到 tb_blog.liked（当前仅 Redis，重启即失）
        return Result.ok(data);
    }
}
