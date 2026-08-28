package com.wanger.blogservice.controller;

import com.wanger.blogservice.entity.Blog;
import com.wanger.blogservice.service.IBlogService;
import com.wanger.common.dto.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 发布探店笔记
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 删除自己的笔记
     */
    @DeleteMapping("/{id}")
    public Result deleteBlog(@PathVariable("id") Long id) {
        return blogService.deleteBlog(id);
    }

    /**
     * 刷关注页（Feed 流）
     * lastTime：上一页最后一条的 score（毫秒），首次不传；pageSize：每页条数
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam(value = "lastTime", required = false) Long lastTime,
                                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return blogService.queryBlogOfFollow(lastTime, pageSize);
    }

    /**
     * 点赞 / 取消点赞
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }
}
