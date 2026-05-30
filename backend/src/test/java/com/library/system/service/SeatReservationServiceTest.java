package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;
import com.library.system.mapper.ReadingRoomMapper;
import com.library.system.mapper.SeatMapper;
import com.library.system.service.impl.SeatReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("SeatReservationService 单元测试")
class SeatReservationServiceTest extends BaseTest {

    @Mock
    private ReadingRoomMapper readingRoomMapper;

    @Mock
    private SeatMapper seatMapper;

    @InjectMocks
    private SeatReservationServiceImpl seatReservationService;

    @Nested
    @DisplayName("阅览室查询")
    class ReadingRoomTests {

        @Test
        @DisplayName("获取阅览室列表 - 应返回列表")
        void getReadingRooms_shouldReturnList() {
            ReadingRoom room = new ReadingRoom();
            room.setId(1L);
            room.setName("一楼阅览室");
            when(readingRoomMapper.selectList(null)).thenReturn(List.of(room));

            List<ReadingRoom> rooms = seatReservationService.getReadingRooms();

            assertNotNull(rooms);
            assertFalse(rooms.isEmpty());
            assertEquals("一楼阅览室", rooms.get(0).getName());
        }

        @Test
        @DisplayName("获取阅览室列表 - 无记录时返回空列表")
        void getReadingRooms_empty_shouldReturnEmptyList() {
            when(readingRoomMapper.selectList(null)).thenReturn(List.of());

            List<ReadingRoom> rooms = seatReservationService.getReadingRooms();

            assertNotNull(rooms);
            assertTrue(rooms.isEmpty());
        }
    }

    @Nested
    @DisplayName("座位查询")
    class SeatQueryTests {

        @Test
        @DisplayName("获取房间座位 - 应返回列表")
        void getSeatsByRoom_shouldReturnList() {
            Seat seat = new Seat();
            seat.setId(1L);
            seat.setSeatNumber("A01");
            seat.setRoomId(1L);
            when(seatMapper.selectByRoomId(1L)).thenReturn(List.of(seat));

            List<Seat> seats = seatReservationService.getSeatsByRoom(1L);

            assertNotNull(seats);
            assertFalse(seats.isEmpty());
            assertEquals("A01", seats.get(0).getSeatNumber());
        }

        @Test
        @DisplayName("获取房间座位 - 无座位时返回空列表")
        void getSeatsByRoom_empty_shouldReturnEmptyList() {
            when(seatMapper.selectByRoomId(999L)).thenReturn(List.of());

            List<Seat> seats = seatReservationService.getSeatsByRoom(999L);

            assertNotNull(seats);
            assertTrue(seats.isEmpty());
        }
    }
}
