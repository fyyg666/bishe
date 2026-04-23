package com.library.system.service;

import com.library.system.dto.SeatReservationRequest;
import com.library.system.dto.SeatReservationResponse;
import com.library.system.entity.SeatReservation;
import com.library.system.mapper.SeatReservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 座位预约服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SeatReservationServiceTest {

    @Mock
    private SeatReservationMapper seatReservationMapper;

    @InjectMocks
    private SeatReservationServiceImpl seatReservationService;

    private SeatReservation testReservation;
    private SeatReservationRequest testRequest;
    private SeatReservationResponse testResponse;

    @BeforeEach
    void setUp() {
        testReservation = new SeatReservation();
        testReservation.setId(1L);
        testReservation.setSeatId(1L);
        testReservation.setReaderId(1L);
        testReservation.setStartTime(LocalDateTime.now().plusHours(1));
        testReservation.setEndTime(LocalDateTime.now().plusHours(5));
        testReservation.setStatus(1);  // 预约成功

        testRequest = SeatReservationRequest.builder()
                .seatId(1L)
                .readerId(1L)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(5))
                .build();

        testResponse = SeatReservationResponse.builder()
                .id(1L)
                .seatId(1L)
                .seatNumber("A001")
                .readerId(1L)
                .readerName("张三")
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(5))
                .status(1)
                .build();
    }

    @Test
    void testReserveSeat_Success() {
        when(seatReservationMapper.insert(any(SeatReservation.class))).thenReturn(1);

        SeatReservationResponse result = seatReservationService.reserveSeat(testRequest);

        assertNotNull(result);
        assertEquals("A001", result.getSeatNumber());
        assertEquals(1, result.getStatus());
        verify(seatReservationMapper).insert(any(SeatReservation.class));
    }

    @Test
    void testReserveSeat_SeatAlreadyReserved() {
        // Mock that seat is already reserved for the time period
        when(seatReservationMapper.selectBySeatIdAndTime(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(testReservation));

        assertThrows(RuntimeException.class, () -> 
                seatReservationService.reserveSeat(testRequest));
        
        verify(seatReservationMapper, never()).insert(any(SeatReservation.class));
    }

    @Test
    void testCancelReservation_Success() {
        testReservation.setStatus(1);  // 预约中
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);
        when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

        boolean result = seatReservationService.cancelReservation(1L, 1L);

        assertTrue(result);
        verify(seatReservationMapper).updateById(any(SeatReservation.class));
    }

    @Test
    void testCancelReservation_NotFound() {
        when(seatReservationMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> 
                seatReservationService.cancelReservation(999L, 1L));
        
        verify(seatReservationMapper, never()).updateById(any(SeatReservation.class));
    }

    @Test
    void testCancelReservation_NotOwner() {
        testReservation.setReaderId(2L);  // 不是当前用户
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        assertThrows(RuntimeException.class, () -> 
                seatReservationService.cancelReservation(1L, 1L));
        
        verify(seatReservationMapper, never()).updateById(any(SeatReservation.class));
    }

    @Test
    void testGetMyReservations_Success() {
        List<SeatReservation> reservations = Arrays.asList(testReservation);
        when(seatReservationMapper.selectByReaderId(1L)).thenReturn(reservations);

        List<SeatReservationResponse> result = seatReservationService.getMyReservations(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getReaderName());
        verify(seatReservationMapper).selectByReaderId(1L);
    }

    @Test
    void testGetMyReservations_EmptyList() {
        when(seatReservationMapper.selectByReaderId(999L)).thenReturn(Arrays.asList());

        List<SeatReservationResponse> result = seatReservationService.getMyReservations(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(seatReservationMapper).selectByReaderId(999L);
    }

    @Test
    void testGetReservationById_Success() {
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        SeatReservationResponse result = seatReservationService.getReservationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("A001", result.getSeatNumber());
        verify(seatReservationMapper).selectById(1L);
    }

    @Test
    void testGetReservationById_NotFound() {
        when(seatReservationMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> 
                seatReservationService.getReservationById(999L));
        
        verify(seatReservationMapper).selectById(999L);
    }

    @Test
    void testCheckIn_Success() {
        testReservation.setStatus(1);  // 预约中
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);
        when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

        boolean result = seatReservationService.checkIn(1L, 1L);

        assertTrue(result);
        verify(seatReservationMapper).updateById(any(SeatReservation.class));
    }

    @Test
    void testCheckIn_NotOwner() {
        testReservation.setReaderId(2L);  // 不是当前用户
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);

        assertThrows(RuntimeException.class, () -> 
                seatReservationService.checkIn(1L, 1L));
        
        verify(seatReservationMapper, never()).updateById(any(SeatReservation.class));
    }

    @Test
    void testCheckOut_Success() {
        testReservation.setStatus(2);  // 已签到
        when(seatReservationMapper.selectById(1L)).thenReturn(testReservation);
        when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

        boolean result = seatReservationService.checkOut(1L, 1L);

        assertTrue(result);
        verify(seatReservationMapper).updateById(any(SeatReservation.class));
    }

    @Test
    void testGetActiveReservations_Success() {
        List<SeatReservation> activeReservations = Arrays.asList(testReservation);
        when(seatReservationMapper.selectActiveReservations()).thenReturn(activeReservations);

        List<SeatReservationResponse> result = seatReservationService.getActiveReservations();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(seatReservationMapper).selectActiveReservations();
    }

    @Test
    void testAutoCancelExpiredReservations_Success() {
        List<SeatReservation> expiredReservations = Arrays.asList(testReservation);
        when(seatReservationMapper.selectExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(expiredReservations);
        when(seatReservationMapper.updateById(any(SeatReservation.class))).thenReturn(1);

        int result = seatReservationService.autoCancelExpiredReservations();

        assertEquals(1, result);
        verify(seatReservationMapper).selectExpiredReservations(any(LocalDateTime.class));
        verify(seatReservationMapper, times(1)).updateById(any(SeatReservation.class));
    }

    @Test
    void testReserveSeat_ValidationError_EndBeforeStart() {
        SeatReservationRequest invalidRequest = SeatReservationRequest.builder()
                .seatId(1L)
                .readerId(1L)
                .startTime(LocalDateTime.now().plusHours(5))  // 开始时间在结束时间之后
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        // This should trigger validation error
        assertThrows(Exception.class, () -> 
                seatReservationService.reserveSeat(invalidRequest));
    }

    @Test
    void testReserveSeat_ValidationError_DurationTooLong() {
        SeatReservationRequest invalidRequest = SeatReservationRequest.builder()
                .seatId(1L)
                .readerId(1L)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(13))  // 超过12小时
                .build();

        // This should trigger validation error (max 12 hours)
        assertThrows(Exception.class, () -> 
                seatReservationService.reserveSeat(invalidRequest));
    }
}
