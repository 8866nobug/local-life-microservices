package com.wanger.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.blogservice.entity.Follow;
import com.wanger.blogservice.mapper.FollowMapper;
import com.wanger.blogservice.service.IFollowService;
import com.wanger.common.dto.Result;
import com.wanger.common.utils.UserHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long followUserId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        if (userId.equals(followUserId)) {
            return Result.fail("不能关注自己");
        }
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        try {
            save(follow);
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_user_follow 兜底，重复关注视为幂等成功
        }
        return Result.ok();
    }

    @Override
    public Result unFollow(Long followUserId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            return Result.fail("未登录");
        }
        remove(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
        return Result.ok();
    }
}
