package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;
import com.library.system.mapper.ReadingRoomMapper;
import com.library.system.mapper.SeatMapper;
import com.library.system.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 座位预约服务实现类
 * <p>
 * 仅提供阅览室查询和座位查询功能。
 * 预约相关业务已迁移至 {@link com.library.system.service.impl.SeatServiceImpl}。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatReservationServiceImpl implements SeatReservationService {

    private final ReadingRoomMapper readingRoomMapper;
    private final SeatMapper seatMapper;

    @Override
    public List<ReadingRoom> getReadingRooms() {
        LambdaQueryWrapper<ReadingRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingRoom::getDeleted, 0);
        return readingRoomMapper.selectList(wrapper);
    }

    @Override
    public List<Seat> getSeatsByRoom(Long roomId) {
        return seatMapper.selectByRoomId(roomId);
    }
}
