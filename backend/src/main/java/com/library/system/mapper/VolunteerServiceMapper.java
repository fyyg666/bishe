package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.entity.VolunteerService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 志愿服务Mapper接口
 *
 * @author Library Team
 * @version 2.0.0
 */
@Mapper
public interface VolunteerServiceMapper extends BaseMapper<VolunteerService> {

    /**
     * 查询用户的志愿服务记录
     */
    Page<VolunteerService> selectByUserId(Page<VolunteerService> page, @Param("userId") Long userId);

    /**
     * 查询待审核的记录
     */
    Page<VolunteerService> selectPendingReview(Page<VolunteerService> page);
}
