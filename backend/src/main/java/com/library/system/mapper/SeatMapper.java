package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 座位Mapper接口
 *
 * @author Library Team
 * @version 2.0.0
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 查询阅览室的座位列表
     */
    @Select("SELECT * FROM seat WHERE room_id = #{roomId} AND deleted = 0")
    List<Seat> selectByRoomId(@Param("roomId") Long roomId);

    /**
     * FOR UPDATE - 悲观锁
     */
    @Select("SELECT * FROM seat WHERE id = #{id} FOR UPDATE")
    Seat selectForUpdate(@Param("id") Long id);
}
