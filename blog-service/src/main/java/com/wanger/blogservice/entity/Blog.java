package com.wanger.blogservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog")
public class Blog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商户id
     */
    private Long shopId;

    /**
     * 用户id（作者）
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 探店的照片，最多9张，多张以","隔开
     */
    private String images;

    /**
     * 探店的文字描述
     */
    private String content;

    /**
     * 点赞数量
     */
    private Integer liked;

    /**
     * 评论数量（本期未实现，预留）
     */
    private Integer comments;

    /**
     * 作者头像（非表字段，Feed 流展示时由 Feign 查 UserDTO 填充）
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 作者昵称（非表字段，Feed 流展示时由 Feign 查 UserDTO 填充）
     */
    @TableField(exist = false)
    private String name;

    /**
     * 当前登录用户是否点赞（非表字段，读时按 Redis 集合判断填充）
     */
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 发布时间戳（毫秒，非表字段），作为收件箱 ZSet 的 score 与滚动分页游标
     */
    @TableField(exist = false)
    private Long score;
}
