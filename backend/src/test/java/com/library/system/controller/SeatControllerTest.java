package com.library.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.SeatReservationRequest;
import com.library.system.dto.SeatReservationResponse;
import com.library.system.entity.Seat;
import com.library.system.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 座位控制器单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(SeatController.class)
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeatService seatService;

    private Seat testSeat;
    private SeatReservationRequest testReservationRequest;
    private SeatReservationResponse testReservationResponse;

    @BeforeEach
    void setUp() {
        testSeat = new Seat();
        testSeat.setId(1L);
        testSeat.setSeatNumber("A001");
        testSeat.setArea("A区");
        testSeat.setHasPower(true);
        testSeat.setHasWindow(true);
        testSeat.setStatus(1);

        testReservationRequest = SeatReservationRequest.builder()
                .seatId(1L)
                .readerId(1L)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(5))
                .build();

        testReservationResponse = SeatReservationResponse.builder()
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
    void testGetAllSeats_Success() throws Exception {
        List<Seat> seats = Arrays.asList(testSeat);
        when(seatService.getAllSeats()).thenReturn(seats);

        mockMvc.perform(get("/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].seatNumber").value("A001"));

        verify(seatService).getAllSeats();
    }

    @Test
    void testGetAvailableSeats_Success() throws Exception {
        List<Seat> availableSeats = Arrays.asList(testSeat);
        when(seatService.getAvailableSeats()).thenReturn(availableSeats);

        mockMvc.perform(get("/seats/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(seatService).getAvailableSeats();
    }

    @Test
    void testGetSeatById_Success() throws Exception {
        when(seatService.getSeatById(1L)).thenReturn(testSeat);

        mockMvc.perform(get("/seats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.seatNumber").value("A001"))
                .andExpect(jsonPath("$.data.area").value("A区"));

        verify(seatService).getSeatById(1L);
    }

    @Test
    void testGetSeatById_NotFound() throws Exception {
        when(seatService.getSeatById(999L)).thenReturn(null);

        mockMvc.perform(get("/seats/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        verify(seatService).getSeatById(999L);
    }

    @Test
    void testReserveSeat_Success() throws Exception {
        when(seatService.reserveSeat(any(SeatReservationRequest.class))).thenReturn(testReservationResponse);

        mockMvc.perform(post("/seats/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReservationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("预约成功"))
                .andExpect(jsonPath("$.data.seatNumber").value("A001"))
                .andExpect(jsonPath("$.data.readerName").value("张三"));

        verify(seatService).reserveSeat(any(SeatReservationRequest.class));
    }

    @Test
    void testReserveSeat_SeatNotFound() throws Exception {
        when(seatService.reserveSeat(any(SeatReservationRequest.class)))
                .thenThrow(new RuntimeException("座位不存在"));

        mockMvc.perform(post("/seats/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReservationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("座位不存在"));
    }

    @Test
    void testReserveSeat_SeatAlreadyReserved() throws Exception {
        when(seatService.reserveSeat(any(SeatReservationRequest.class)))
                .thenThrow(new RuntimeException("座位已被预约"));

        mockMvc.perform(post("/seats/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReservationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("座位已被预约"));
    }

    @Test
    void testCancelReservation_Success() throws Exception {
        doNothing().when(seatService).cancelReservation(anyLong(), anyLong());

        mockMvc.perform(post("/seats/reservations/1/cancel")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("取消预约成功"));

        verify(seatService).cancelReservation(1L, 1L);
    }

    @Test
    void testGetMyReservations_Success() throws Exception {
        List<SeatReservationResponse> reservations = Arrays.asList(testReservationResponse);
        when(seatService.getMyReservations(anyLong())).thenReturn(reservations);

        mockMvc.perform(get("/seats/my-reservations")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(seatService).getMyReservations(1L);
    }

    @Test
    void testCreateSeat_Success() throws Exception {
        when(seatService.createSeat(any(Seat.class))).thenReturn(testSeat);

        mockMvc.perform(post("/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSeat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建座位成功"));

        verify(seatService).createSeat(any(Seat.class));
    }

    @Test
    void testUpdateSeat_Success() throws Exception {
        when(seatService.updateSeat(any(Seat.class))).thenReturn(testSeat);

        mockMvc.perform(put("/seats/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSeat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新座位成功"));

        verify(seatService).updateSeat(any(Seat.class));
    }

    @Test
    void testDeleteSeat_Success() throws Exception {
        doNothing().when(seatService).deleteSeat(anyLong());

        mockMvc.perform(delete("/seats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除座位成功"));

        verify(seatService).deleteSeat(1L);
    }

    @Test
    void testReserveSeat_ValidationError_InvalidTime() throws Exception {
        SeatReservationRequest invalidRequest = SeatReservationRequest.builder()
                .seatId(1L)
                .readerId(1L)
                .startTime(LocalDateTime.now().plusHours(5))  // End before start
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        mockMvc.perform(post("/seats/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
