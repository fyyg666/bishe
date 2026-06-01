package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.common.Constants;
import com.library.system.dto.SeatReservationRequest;
import com.library.system.dto.SeatReservationResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;
import com.library.system.entity.SeatReservation;
import com.library.system.entity.User;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.ReadingRoomMapper;
import com.library.system.mapper.SeatMapper;
import com.library.system.mapper.SeatReservationMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.SeatServiceImpl;
import com.library.system.template.DistributedLockTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SeatService 单元测试")
class SeatServiceTest extends BaseTest {

    @Mock
    private SeatReservationMapper seatReservationMapper;

    @Mock
    private SeatMapper seatMapper;

    @Mock
    private ReadingRoomMapper readingRoomMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditService creditService;

    @Mock
    private DistributedLockTemplate lockTemplate;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private SeatServiceImpl seatService;

    private User testUser;
    private SeatReservation testReservation;
    private SeatReservationRequest reserveRequest;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("reader1");
        testUser.setRealName("张三");
        testUser.setStatus("NORMAL");
        testUser.setViolationCount(0);

        testReservation = SeatReservation.builder()
                .id(100L)
                .userId(1L)
                .username("reader1")
                .seatNumber("A01")
                .area("A区-安静区")
                .reservationDate(LocalDate.now())
                .startTime(LocalTime.now().minusMinutes(5))
                .endTime(LocalTime.now().plusHours(2))
                .status("PENDING")
                .createTime(LocalDateTime.now())
                .deleted(0)
                .build();

        reserveRequest = new SeatReservationRequest();
        reserveRequest.setSeatNumber("A01");
        reserveRequest.setArea("A区-安静区");
        reserveRequest.setReservationDate(LocalDate.now());
        reserveRequest.setStartTime(LocalTime.of(9, 0));
        reserveRequest.setEndTime(LocalTime.of(11, 0));

        // 通用 mock：让 lockTemplate 实际执行 supplier
        lenient().when(lockTemplate.executeWithLock(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
    }

    @Nested
    @DisplayName("座位查询用例")
    class SeatQueryTests {

        @Test
        @DisplayName("列出座位 - 应返回区域座位列表")
        void listSeats_shouldReturnList() {
            when(seatReservationMapper.selectBySeatNumbersAndDate(anyList(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            List<SeatReservationResponse> seats = seatService.listSeats("A区-安静区", LocalDate.now());

            assertNotNull(seats);
            assertEquals(50, seats.size());
            assertTrue(seats.stream().allMatch(s -> s.getStatus().equals("AVAILABLE")));
        }

        @Test
        @DisplayName("列出所有区域座位 - 不传区域时返回全部")
        void listSeats_allAreas_shouldReturnAll() {
            when(seatReservationMapper.selectBySeatNumbersAndDate(anyList(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            List<SeatReservationResponse> seats = seatService.listSeats(null, LocalDate.now());

            assertNotNull(seats);
            assertEquals(150, seats.size());
        }

        @Test
        @DisplayName("获取阅览室列表")
        void getReadingRooms_shouldReturnList() {
            ReadingRoom room = new ReadingRoom();
            room.setId(1L);
            room.setName("一楼阅览室");
            when(seatReservationService.getReadingRooms()).thenReturn(List.of(room));

            List<ReadingRoom> rooms = seatService.getReadingRooms();

            assertNotNull(rooms);
            assertFalse(rooms.isEmpty());
            assertEquals("一楼阅览室", rooms.get(0).getName());
        }

        @Test
        @DisplayName("获取房间座位列表")
        void getSeatsByRoom_shouldReturnList() {
            Seat seat = new Seat();
            seat.setId(1L);
            seat.setSeatNumber("A01");
            when(seatReservationService.getSeatsByRoom(1L)).thenReturn(List.of(seat));

            List<Seat> seats = seatService.getSeatsByRoom(1L);

            assertNotNull(seats);
            assertFalse(seats.isEmpty());
        }

        @Test
        @DisplayName("查询时间段可用性 - 无冲突返回true")
        void isTimeSlotAvailable_noConflict_shouldReturnTrue() {
            when(seatReservationMapper.countConflictingReservations(
                    anyString(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(0);

            boolean available = seatService.isTimeSlotAvailable("A01", LocalDate.now(), "09:00", "11:00");

            assertTrue(available);
        }

        @Test
        @DisplayName("查询时间段可用性 - 有冲突返回false")
        void isTimeSlotAvailable_withConflict_shouldReturnFalse() {
            when(seatReservationMapper.countConflictingReservations(
                    anyString(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(1);

            boolean available = seatService.isTimeSlotAvailable("A01", LocalDate.now(), "09:00", "11:00");

            assertFalse(available);
        }
    }

    @Nested
    @DisplayName("座位预约用例")
    class ReservationTests {

        @Test
        @DisplayName("预约座位成功")
        void reserveSeat_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(seatReservationMapper.countConflictingReservations(
                    anyString(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(0);
            when(seatReservationMapper.selectByUserAndDate(anyLong(), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(seatReservationMapper.insert(any(SeatReservation.class))).thenReturn(1);

            SeatReservationResponse response = seatService.reserveSeat(1L, reserveRequest);

            assertNotNull(response);
            assertEquals("A01", response.getSeatNumber());
            assertEquals("PENDING", response.getStatus());
        }

        @Test
        @DisplayName("预约座位 - 用户不存在抛异常")
        void reserveSeat_userNotFound_shouldThrowException() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> seatService.reserveSeat(999L, reserveRequest));
        }

        @Test
        @DisplayName("预约座位 - 时间冲突抛异常")
        void reserveSeat_timeConflict_shouldThrowException() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(seatReservationMapper.countConflictingReservations(
                    anyString(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(1);

            assertThrows(BusinessException.class,
                    () -> seatService.reserveSeat(1L, reserveRequest));
        }
    }

    @Nested
    @DisplayName("签到签退用例")
    class CheckInOutTests {

        @Test
        @DisplayName("签到成功")
        void checkIn_success() {
            when(seatReservationMapper.selectById(100L)).thenReturn(testReservation);
            doNothing().when(creditService).processCheckInCredit(anyLong(), anyLong());
            when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

            SeatReservationResponse response = seatService.checkIn(1L, 100L);

            assertNotNull(response);
            verify(creditService).processCheckInCredit(1L, 100L);
        }

        @Test
        @DisplayName("签到 - 预约不存在抛异常")
        void checkIn_reservationNotFound_shouldThrowException() {
            when(seatReservationMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> seatService.checkIn(1L, 999L));
        }

        @Test
        @DisplayName("签到 - 无权操作抛异常")
        void checkIn_notOwner_shouldThrowException() {
            when(seatReservationMapper.selectById(100L)).thenReturn(testReservation);

            assertThrows(ForbiddenException.class,
                    () -> seatService.checkIn(2L, 100L));
        }

        @Test
        @DisplayName("签退成功")
        void checkOut_success() {
            testReservation.setStatus("CHECKED_IN");
            when(seatReservationMapper.selectById(100L)).thenReturn(testReservation);
            when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

            SeatReservationResponse response = seatService.checkOut(1L, 100L);

            assertNotNull(response);
            verify(seatReservationMapper).updateById(argThat(r -> "COMPLETED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("签退 - 未签到状态抛异常")
        void checkOut_notCheckedIn_shouldThrowException() {
            when(seatReservationMapper.selectById(100L)).thenReturn(testReservation);

            assertThrows(BusinessException.class,
                    () -> seatService.checkOut(1L, 100L));
        }
    }

    @Nested
    @DisplayName("预约取消用例")
    class CancelTests {

        @Test
        @DisplayName("取消预约成功")
        void cancelReservation_success() {
            // 设置预约时间为未来2小时后，使取消成功
            testReservation.setReservationDate(LocalDate.now().plusDays(1));
            when(seatReservationMapper.selectById(100L)).thenReturn(testReservation);
            when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

            seatService.cancelReservation(1L, 100L);

            verify(seatReservationMapper).updateById(argThat(r -> "CANCELLED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("取消预约 - 预约不存在抛异常")
        void cancelReservation_notFound_shouldThrowException() {
            when(seatReservationMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> seatService.cancelReservation(1L, 999L));
        }

        @Test
        @DisplayName("取消预约 - 无权操作抛异常")
        void cancelReservation_notOwner_shouldThrowException() {
            when(seatReservationMapper.selectById(100L)).thenReturn(testReservation);

            assertThrows(ForbiddenException.class,
                    () -> seatService.cancelReservation(2L, 100L));
        }
    }

    @Nested
    @DisplayName("预约查询用例")
    class MyReservationsTests {

        @Test
        @DisplayName("获取我的预约列表")
        void getMyReservations_shouldReturnPage() {
            when(seatReservationMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<SeatReservation> p = inv.getArgument(0);
                p.setRecords(Collections.singletonList(testReservation));
                p.setTotal(1);
                return p;
            });

            PageResult<SeatReservationResponse> result = seatService.getMyReservations(1L, 1L, 10L);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("获取我的预约列表 - 无记录")
        void getMyReservations_empty_shouldReturnEmpty() {
            when(seatReservationMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<SeatReservation> p = inv.getArgument(0);
                p.setRecords(Collections.emptyList());
                p.setTotal(0);
                return p;
            });

            PageResult<SeatReservationResponse> result = seatService.getMyReservations(1L, 1L, 10L);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("预约边界校验用例")
    class ValidationBoundaryTests {

        @Test
        @DisplayName("预约座位 - 开始时间晚于结束时间抛异常")
        void reserveSeat_startAfterEnd_shouldThrow() {
            reserveRequest.setStartTime(LocalTime.of(14, 0));
            reserveRequest.setEndTime(LocalTime.of(13, 0));
            when(userMapper.selectById(1L)).thenReturn(testUser);

            assertThrows(BusinessException.class,
                    () -> seatService.reserveSeat(1L, reserveRequest));
        }

        @Test
        @DisplayName("预约座位 - 开始时间等于结束时间抛异常")
        void reserveSeat_startEqualsEnd_shouldThrow() {
            reserveRequest.setStartTime(LocalTime.of(10, 0));
            reserveRequest.setEndTime(LocalTime.of(10, 0));
            when(userMapper.selectById(1L)).thenReturn(testUser);

            assertThrows(BusinessException.class,
                    () -> seatService.reserveSeat(1L, reserveRequest));
        }

        @Test
        @DisplayName("预约座位 - 过去日期抛异常")
        void reserveSeat_pastDate_shouldThrow() {
            reserveRequest.setReservationDate(LocalDate.now().minusDays(1));
            when(userMapper.selectById(1L)).thenReturn(testUser);

            assertThrows(BusinessException.class,
                    () -> seatService.reserveSeat(1L, reserveRequest));
        }

        @Test
        @DisplayName("预约座位 - 超过每日预约上限抛异常")
        void reserveSeat_exceedDailyLimit_shouldThrow() {
            testUser.setViolationCount(0);
            testUser.setBanUntil(null);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any()))
                    .thenReturn(0);
            when(seatReservationMapper.selectByUserAndDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(Collections.nCopies(Constants.SeatLimit.DAILY_MAX_RESERVATIONS, testReservation));

            assertThrows(BusinessException.class,
                    () -> seatService.reserveSeat(1L, reserveRequest));
        }

        @Test
        @DisplayName("预留座位 - 用户被临时封禁抛异常")
        void reserveSeat_userBanned_shouldThrow() {
            testUser.setBanUntil(LocalDateTime.now().plusHours(24));
            testUser.setViolationCount(3);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            assertThrows(BusinessException.class,
                    () -> seatService.reserveSeat(1L, reserveRequest));
        }
    }
}
