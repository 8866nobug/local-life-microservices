package com.wanger.blogservice.controller;

import com.wanger.blogservice.service.IFollowService;
import com.wanger.common.dto.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注
     */
    @PostMapping("/{followUserId}")
    public Result follow(@PathVariable("followUserId") Long followUserId) {
        return followService.follow(followUserId);
    }

    /**
     * 取关
     */
    @DeleteMapping("/{followUserId}")
    public Result unFollow(@PathVariable("followUserId") Long followUserId) {
        return followService.unFollow(followUserId);
    }
}
