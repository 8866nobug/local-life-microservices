-- ----------------------------
-- blog-service 数据库初始化脚本（dev_blog 库）
-- 表：tb_blog（探店笔记）、tb_follow（关注关系）
-- ----------------------------

-- ----------------------------
-- Table structure for tb_blog
-- ----------------------------
DROP TABLE IF EXISTS `tb_blog`;
CREATE TABLE `tb_blog`  (
    `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shop_id` bigint(20) NOT NULL COMMENT '商户id',
    `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id（作者）',
    `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
    `images` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '探店的照片，最多9张，多张以","隔开',
    `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '探店的文字描述',
    `liked` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '点赞数量',
    `comments` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '评论数量',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_blog（测试种子数据，可删除）
-- ----------------------------
INSERT INTO `tb_blog` (`id`, `shop_id`, `user_id`, `title`, `images`, `content`, `liked`, `comments`, `create_time`, `update_time`) VALUES
(1, 1, 1, '这家火锅店绝了', '/imgs/blogs/blog1.jpg', '锅底香、食材新鲜，强烈推荐', 0, 0, '2026-08-29 10:00:00', '2026-08-29 10:00:00'),
(2, 2, 1, '周末探店咖啡店', '/imgs/blogs/blog2.jpg', '环境安静，适合办公', 0, 0, '2026-08-29 11:00:00', '2026-08-29 11:00:00');

-- ----------------------------
-- Table structure for tb_follow
-- ----------------------------
DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow`  (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id（关注者）',
    `follow_user_id` bigint(20) UNSIGNED NOT NULL COMMENT '被关注用户id',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_follow（测试种子数据，可删除）
-- ----------------------------
INSERT INTO `tb_follow` (`user_id`, `follow_user_id`) VALUES (2, 1);
