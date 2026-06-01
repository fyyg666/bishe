package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.entity.SeatReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 座位预约数据访问层
 */
@Mapper
public interface SeatReservationMapper extends BaseMapper<SeatReservation> {

    /**
     * 查询用户的预约记录（分页）
     */
    @Select("SELECT * FROM seat_reservation WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY reservation_date DESC, start_time DESC")
    IPage<SeatReservation> selectByUserId(Page<SeatReservation> page, @Param("userId") Long userId);

    /**
     * 查询指定座位当天的预约记录（按座位编号）
     */
    @Select("SELECT sr.* FROM seat_reservation sr " +
            "INNER JOIN seat s ON sr.seat_id = s.id " +
            "WHERE s.seat_number = #{seatNumber} " +
            "AND sr.reservation_date = #{date} AND sr.status IN ('PENDING', 'CHECKED_IN') AND sr.deleted = 0")
    List<SeatReservation> selectBySeatAndDate(@Param("seatNumber") String seatNumber,
                                              @Param("date") LocalDate date);

    /**
     * 批量查询多个座位当天的预约记录（PERF-002优化，避免N+1）
     */
    @Select("<script>" +
            "SELECT sr.* FROM seat_reservation sr " +
            "INNER JOIN seat s ON sr.seat_id = s.id " +
            "WHERE s.seat_number IN " +
            "<foreach collection='seatNumbers' item='sn' open='(' separator=',' close=')'>" +
            "#{sn}" +
            "</foreach>" +
            " AND sr.reservation_date = #{date} AND sr.status IN ('PENDING', 'CHECKED_IN') AND sr.deleted = 0" +
            "</script>")
    List<SeatReservation> selectBySeatNumbersAndDate(@Param("seatNumbers") List<String> seatNumbers,
                                                    @Param("date") LocalDate date);

    /**
     * 检查时间段是否冲突（按座位编号）
     */
    @Select("SELECT COUNT(*) FROM seat_reservation sr " +
            "INNER JOIN seat s ON sr.seat_id = s.id " +
            "WHERE s.seat_number = #{seatNumber} " +
            "AND sr.reservation_date = #{date} AND sr.status IN ('PENDING', 'CHECKED_IN') " +
            "AND sr.deleted = 0 AND ((sr.start_time < #{endTime} AND sr.end_time > #{startTime}))")
    int countConflictingReservations(@Param("seatNumber") String seatNumber,
                                     @Param("date") LocalDate date,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime);

    /**
     * 查询用户当天的预约
     */
    @Select("SELECT * FROM seat_reservation WHERE user_id = #{userId} " +
            "AND reservation_date = #{date} AND deleted = 0")
    List<SeatReservation> selectByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 统计用户每日预约次数
     */
    @Select("SELECT COUNT(*) FROM seat_reservation WHERE user_id = #{userId} " +
            "AND reservation_date = #{date} AND status NOT IN ('CANCELLED', 'VIOLATED') AND deleted = 0")
    Integer countUserDailyReservations(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 统计用户今日预约次数
     */
    default Integer countUserTodayReservations(Long userId, LocalDate date) {
        return countUserDailyReservations(userId, date);
    }

    /**
     * 查询座位活跃预约（按座位编号）
     */
    @Select("SELECT sr.* FROM seat_reservation sr " +
            "INNER JOIN seat s ON sr.seat_id = s.id " +
            "WHERE s.seat_number = #{seatNumber} " +
            "AND sr.reservation_date = #{date} " +
            "AND sr.status IN ('PENDING', 'CHECKED_IN') AND sr.deleted = 0")
    List<SeatReservation> selectActiveBySeatId(@Param("seatNumber") String seatNumber,
                                                @Param("date") LocalDate date);
}
