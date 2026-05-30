package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.Compensation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 赔偿订单Mapper
 *
 * @author Library Team
 * @version 2.0.0
 */
@Mapper
public interface CompensationMapper extends BaseMapper<Compensation> {
}
