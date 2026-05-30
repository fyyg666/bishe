package com.library.system.service;

import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;

import java.util.List;

/**
 * 座位预约服务接口
 * <p>
 * 提供阅览室列表获取和座位查询功能。
 * 预约相关操作由 {@link SeatService} 统一管理。
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
}
