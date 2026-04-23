package com.library.system.service;

import com.library.system.dto.*;
import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;

import java.time.LocalDate;
import java.util.List;

/**
 * 座位服务接口
 * <p>
 * 提供阅览座位的查询、预约、取消、签到、签退等操作。
 * 支持按区域和日期查询座位可用性，并检查时间段冲突。
 * FIXED: BIZ-001 合并 SeatReservationService 的独有方法到此接口
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface SeatService {

    /**
     * 获取座位列表
     *
     * @param area 区域名称
     * @param date 日期
     * @return 座位列表
     */
    List<SeatReservationResponse> listSeats(String area, LocalDate date);

    /**
     * 预约座位
     *
     * @param userId 用户ID
     * @param request 预约请求
     * @return 预约记录
     */
    SeatReservationResponse reserveSeat(Long userId, SeatReservationRequest request);

    /**
     * 取消预约
     *
     * @param userId 用户ID
     * @param reservationId 预约ID
     */
    void cancelReservation(Long userId, Long reservationId);

    /**
     * 签到
     *
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @return 预约记录
     */
    SeatReservationResponse checkIn(Long userId, Long reservationId);

    /**
     * 签退
     *
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @return 预约记录
     */
    SeatReservationResponse checkOut(Long userId, Long reservationId);

    /**
     * 获取我的预约列表
     *
     * @param userId 用户ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<SeatReservationResponse> getMyReservations(Long userId, Long current, Long size);

    /**
     * 检查时间段是否可用
     *
     * @param seatNumber 座位编号
     * @param date 日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否可用
     */
    boolean isTimeSlotAvailable(String seatNumber, LocalDate date, String startTime, String endTime);

    /**
     * 获取阅览室列表
     *
     * @return 阅览室列表
     */
    List<ReadingRoom> getReadingRooms();

    /**
     * 获取阅览室的座位列表
     *
     * @param roomId 阅览室ID
     * @return 座位列表
     */
    List<Seat> getSeatsByRoom(Long roomId);
}
