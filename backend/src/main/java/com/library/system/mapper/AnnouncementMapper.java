package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告Mapper接口
 *
 * @author Library Team
 * @version 2.0.0
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
