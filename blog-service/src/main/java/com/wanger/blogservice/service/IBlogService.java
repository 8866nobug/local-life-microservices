package com.wanger.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanger.blogservice.dto.FeedPushMessage;
import com.wanger.blogservice.entity.Blog;
import com.wanger.common.dto.Result;

public interface IBlogService extends IService<Blog> {

    /**
     * 发布探店笔记
     */
    Result saveBlog(Blog blog);

    /**
     * 删除自己的笔记
     */
    Result deleteBlog(Long id);

    /**
     * 按 id 查笔记（走内容缓存，防穿透），供 Feed 流回源使用
     */
    Blog queryBlogById(Long id);

    /**
     * MQ 消费者回调：把笔记写入作者所有粉丝的收件箱（写扩散）
     */
    void pushToInbox(FeedPushMessage message);

    /**
     * 刷关注页（Feed 流）：推（收件箱）+ 拉（关注的大V）合并，滚动分页
     *
     * @param lastTime 上一页最后一条的 score（毫秒时间戳），首次不传默认当前时间
     * @param pageSize 每页条数
     */
    Result queryBlogOfFollow(Long lastTime, Integer pageSize);

    /**
     * 点赞 / 取消点赞（Redis Set 幂等去重）
     */
    Result likeBlog(Long id);
}
