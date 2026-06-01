package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.BookReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BookReservationMapper extends BaseMapper<BookReservation> {

    @Select("SELECT COUNT(*) FROM book_reservation WHERE book_id = #{bookId} AND status = 'PENDING' AND deleted = 0")
    int countPendingByBookId(@Param("bookId") Long bookId);
}
