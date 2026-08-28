package com.wanger.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanger.blogservice.entity.Follow;
import com.wanger.common.dto.Result;

public interface IFollowService extends IService<Follow> {

    /**
     * 关注
     */
    Result follow(Long followUserId);

    /**
     * 取关
     */
    Result unFollow(Long followUserId);
}
