package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.CreditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 信用积分日志数据访问层
 */
@Mapper
public interface CreditLogMapper extends BaseMapper<CreditLog> {

    /**
     * 查询用户的积分日志
     */
    @Select("SELECT * FROM credit_log WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<CreditLog> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户最新的积分记录
     */
    @Select("SELECT * FROM credit_log WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
    CreditLog selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 统计用户积分变动
     */
    @Select("SELECT SUM(change_value) FROM credit_log WHERE user_id = #{userId}")
    Integer sumCreditChangeByUserId(@Param("userId") Long userId);
}
