package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.SeatReservationRequest;
import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;
import com.library.system.entity.SeatReservation;

import java.util.List;

/**
 * 座位预约服务接口
 * <p>
 * 提供阅览室管理和座位预约的核心业务逻辑，包括阅览室列表获取、
 * 座位查询、预约创建、签到/签退和预约取消等操作。
 * 与 {@link SeatService} 配合使用，SeatService面向Controller层，
 * 本接口面向底层业务实现。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface SeatReservationService {

    /**
     * 获取阅览室列表
     */
    List<ReadingRoom> getReadingRooms();

    /**
     * 获取阅览室的座位列表
     */
    List<Seat> getSeatsByRoom(Long roomId);

    /**
     * 预约座位
     */
    SeatReservation reserveSeat(Long userId, SeatReservationRequest request);

    /**
     * 签到
     */
    SeatReservation checkIn(Long userId, Long reservationId);

    /**
     * 签退
     */
    SeatReservation checkOut(Long userId, Long reservationId);

    /**
     * 取消预约
     */
    void cancelReservation(Long userId, Long reservationId);

    /**
     * 获取我的预约记录
     */
    PageResult<SeatReservation> getMyReservations(Long userId, int pageNum, int pageSize);
}
