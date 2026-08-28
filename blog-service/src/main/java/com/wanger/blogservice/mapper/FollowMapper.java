package com.wanger.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wanger.blogservice.entity.Follow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 查询粉丝数超过阈值的用户 id（大V），用于启动预热重建大V列表。
     * 生产数据量大时可分批/分页，这里直接 group by 取超阈值者。
     */
    @Select("SELECT follow_user_id FROM tb_follow GROUP BY follow_user_id HAVING COUNT(*) > #{threshold}")
    List<Long> selectBigVIds(@Param("threshold") long threshold);
}
