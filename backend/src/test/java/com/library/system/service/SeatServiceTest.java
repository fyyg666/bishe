package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.SeatReservationRequest;
import com.library.system.dto.SeatReservationResponse;
import com.library.system.entity.SeatReservation;
import com.library.system.entity.User;
import com.library.system.mapper.SeatReservationMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.SeatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 座位服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatReservationMapper seatReservationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditService creditService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private SeatServiceImpl seatService;

    private User testUser;
    private SeatReservation testReservation;
    private SeatReservationRequest testReservationRequest;

    @BeforeEach
    void setUp() throws InterruptedException {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        testUser.setStatus("NORMAL");
        testUser.setCreditScore(100);

        // 初始化测试预约记录
        testReservation = new SeatReservation();
        testReservation.setId(1L);
        testReservation.setSeatNumber("A01");
        testReservation.setArea("A区-安静区");
        testReservation.setUserId(1L);
        testReservation.setUsername("testuser");
        testReservation.setReservationDate(LocalDate.now().plusDays(1));
        testReservation.setStartTime(LocalTime.of(9, 0));
        testReservation.setEndTime(LocalTime.of(12, 0));
        testReservation.setStatus("PENDING");
        testReservation.setSource("WEB");
        testReservation.setDeleted(0);

        // 初始化预约请求
        testReservationRequest = new SeatReservationRequest();
        testReservationRequest.setSeatNumber("A01");
        testReservationRequest.setArea("A区-安静区");
        testReservationRequest.setReservationDate(LocalDate.now().plusDays(1));
        testReservationRequest.setStartTime(LocalTime.of(9, 0));
        testReservationRequest.setEndTime(LocalTime.of(12, 0));

        // Mock分布式锁
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void testListSeats_AllAreas() {
        when(seatReservationMapper.selectBySeatAndDate(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<SeatReservationResponse> result = seatService.listSeats(null, LocalDate.now());

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testListSeats_SpecificArea() {
        when(seatReservationMapper.selectBySeatAndDate(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<SeatReservationResponse> result = seatService.listSeats("A区-安静区", LocalDate.now());

        assertNotNull(result);
        assertTrue(result.stream().allMatch(s -> s.getArea().equals("A区-安静区")));
    }

    @Test
    void testListSeats_WithReservations() {
        List<SeatReservation> reservations = Arrays.asList(testReservation);
        when(seatReservationMapper.selectBySeatAndDate(eq("A01"), any(LocalDate.class)))
                .thenReturn(reservations);

        List<SeatReservationResponse> result = seatService.listSeats("A区-安静区", LocalDate.now());

        assertNotNull(result);
        SeatReservationResponse seat = result.stream()
                .filter(s -> s.getSeatNumber().equals("A01"))
                .findFirst()
                .orElse(null);
        assertNotNull(seat);
        assertEquals("PARTIAL", seat.getStatus());
    }

    @Test
    void testReserveSeat_Success() throws InterruptedException {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any()))
                .thenReturn(0);
        when(seatReservationMapper.selectByUserAndDate(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        when(seatReservationMapper.insert(any(SeatReservation.class))).thenReturn(1);

        SeatReservationResponse result = seatService.reserveSeat(1L, testReservationRequest);

        assertNotNull(result);
        assertEquals("A01", result.getSeatNumber());
        assertEquals("PENDING", result.getStatus());
        verify(seatReservationMapper).insert(any(SeatReservation.class));
    }

    @Test
    void testReserveSeat_UserNotFound() throws InterruptedException {
        when(userMapper.selectById(1L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.reserveSeat(1L, testReservationRequest));
        assertEquals("用户不存在或已被禁用", exception.getMessage());
    }

    @Test
    void testReserveSeat_UserDisabled() throws InterruptedException {
        testUser.setStatus("DISABLED");
        when(userMapper.selectById(1L)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.reserveSeat(1L, testReservationRequest));
        assertEquals("用户不存在或已被禁用", exception.getMessage());
    }

    @Test
    void testReserveSeat_PastDate() throws InterruptedException {
        testReservationRequest.setReservationDate(LocalDate.now().minusDays(1));
        when(userMapper.selectById(1L)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.reserveSeat(1L, testReservationRequest));
        assertEquals("不能预约过去的日期", exception.getMessage());
    }

    @Test
    void testReserveSeat_InvalidTimeRange() throws InterruptedException {
        testReservationRequest.setStartTime(LocalTime.of(14, 0));
        testReservationRequest.setEndTime(LocalTime.of(12, 0));
        when(userMapper.selectById(1L)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.reserveSeat(1L, testReservationRequest));
        assertEquals("结束时间必须晚于开始时间", exception.getMessage());
    }

    @Test
    void testReserveSeat_OutsideBusinessHours() throws InterruptedException {
        testReservationRequest.setStartTime(LocalTime.of(7, 0));
        testReservationRequest.setEndTime(LocalTime.of(23, 0));
        when(userMapper.selectById(1L)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.reserveSeat(1L, testReservationRequest));
        assertEquals("预约时间必须在8:00-22:00之间", exception.getMessage());
    }

    @Test
    void testReserveSeat_TimeSlotConflict() throws InterruptedException {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any()))
                .thenReturn(1);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.reserveSeat(1L, testReservationRequest));
        assertEquals("该时间段已被预约", exception.getMessage());
    }

    @Test
    void testReserveSeat_MaxDailyReservations() throws InterruptedException {
        List<SeatReservation> reservations = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SeatReservation r = new SeatReservation();
            r.setStatus("PENDING");
            reservations.add(r);
        }
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any()))
                .thenReturn(0);
        when(seatReservationMapper.selectByUserAndDate(anyLong(), any()))
                .thenReturn(reservations);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.reserveSeat(1L, testReservationRequest));
        assertEquals("每天最多预约3个时段", exception.getMessage());
    }

    @Test
    void testCancelReservation_Success() {
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);
        when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

        assertDoesNotThrow(() -> seatService.cancelReservation(1L, 1L));

        verify(seatReservationMapper).updateById(any(SeatReservation.class));
    }

    @Test
    void testCancelReservation_NotFound() {
        when(seatReservationMapper.selectById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.cancelReservation(1L, 999L));
        assertEquals("预约记录不存在", exception.getMessage());
    }

    @Test
    void testCancelReservation_WrongUser() {
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.cancelReservation(999L, 1L));
        assertEquals("无权操作此预约记录", exception.getMessage());
    }

    @Test
    void testCancelReservation_AlreadyCheckedIn() {
        testReservation.setStatus("CHECKED_IN");
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.cancelReservation(1L, 1L));
        assertEquals("只能取消待使用的预约", exception.getMessage());
    }

    @Test
    void testCheckIn_Success() throws InterruptedException {
        testReservation.setReservationDate(LocalDate.now());
        testReservation.setStartTime(LocalTime.now().minusMinutes(10));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);
        when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

        SeatReservationResponse result = seatService.checkIn(1L, 1L);

        assertNotNull(result);
        assertEquals("CHECKED_IN", result.getStatus());
        verify(creditService).processCheckInCredit(eq(1L), eq(1L));
    }

    @Test
    void testCheckIn_TooEarly() throws InterruptedException {
        testReservation.setReservationDate(LocalDate.now());
        testReservation.setStartTime(LocalTime.now().plusHours(1));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.checkIn(1L, 1L));
        assertEquals("签到时间未到，请在预约开始前15分钟内签到", exception.getMessage());
    }

    @Test
    void testCheckIn_TooLate() throws InterruptedException {
        testReservation.setReservationDate(LocalDate.now());
        testReservation.setStartTime(LocalTime.now().minusHours(1));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.checkIn(1L, 1L));
        assertEquals("已超过签到时间，预约已失效", exception.getMessage());
    }

    @Test
    void testCheckOut_Success() {
        testReservation.setStatus("CHECKED_IN");
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);
        when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

        SeatReservationResponse result = seatService.checkOut(1L, 1L);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void testCheckOut_NotCheckedIn() {
        testReservation.setStatus("PENDING");
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> seatService.checkOut(1L, 1L));
        assertEquals("请先签到后再签退", exception.getMessage());
    }

    @Test
    void testGetMyReservations_Success() {
        List<SeatReservation> reservations = Arrays.asList(testReservation);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SeatReservation> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(reservations);
        page.setTotal(1);

        when(seatReservationMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<SeatReservationResponse> result = seatService.getMyReservations(1L, 1L, 10L);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testIsTimeSlotAvailable_Available() {
        when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any()))
                .thenReturn(0);

        boolean result = seatService.isTimeSlotAvailable("A01", LocalDate.now(), "09:00", "12:00");

        assertTrue(result);
    }

    @Test
    void testIsTimeSlotAvailable_NotAvailable() {
        when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any()))
                .thenReturn(1);

        boolean result = seatService.isTimeSlotAvailable("A01", LocalDate.now(), "09:00", "12:00");

        assertFalse(result);
    }
}
