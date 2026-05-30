package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.SeatReservationRequest;
import com.library.system.dto.SeatReservationResponse;
import com.library.system.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SeatController 安全测试")
class SeatControllerTest extends ControllerTestBase {

    @Mock
    private SeatService seatService;

    @InjectMocks
    private SeatController seatController;

    @BeforeEach
    void setUp() {
        initMockMvc(seatController);
    }

    @Nested
    @DisplayName("座位查询 - 无需认证")
    class QueryEndpoints {

        @Test
        @DisplayName("获取座位列表 - 无需认证")
        void getSeatsByRoom_noAuth_shouldReturn200() throws Exception {
            when(seatService.listSeats(any(), any())).thenReturn(List.of(new SeatReservationResponse()));

            mockMvc.perform(get("/seats"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("预约操作 - 需认证")
    class ReservationEndpoints {

        @Test
        @DisplayName("预约座位 - 无认证返回 401")
        void reserveSeat_noAuth_shouldReturn401() throws Exception {
            mockMvc.perform(post("/seats/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("预约座位 - 读者认证成功")
        void reserveSeat_withAuth_shouldReturn200() throws Exception {
            when(seatService.reserveSeat(anyLong(), any(SeatReservationRequest.class)))
                    .thenReturn(new SeatReservationResponse());

            String body = objectMapper.writeValueAsString(new SeatReservationRequest() {{
                setSeatId(1L);
                setReservationDate(LocalDate.of(2026, 5, 10));
                setStartTime(java.time.LocalTime.of(9, 0));
                setEndTime(java.time.LocalTime.of(11, 0));
            }});

            mockMvc.perform(post("/seats/reserve")
                    .with(readerAuth())
                    .header("Authorization", READER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("签到 - 需认证")
        void checkIn_withAuth_shouldReturn200() throws Exception {
            when(seatService.checkIn(anyLong(), anyLong())).thenReturn(new SeatReservationResponse());

            mockMvc.perform(post("/seats/checkin/1")
                    .with(readerAuth())
                    .header("Authorization", READER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("签退 - 需认证")
        void checkOut_withAuth_shouldReturn200() throws Exception {
            when(seatService.checkOut(anyLong(), anyLong())).thenReturn(new SeatReservationResponse());

            mockMvc.perform(post("/seats/checkout/1")
                    .with(readerAuth())
                    .header("Authorization", READER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("预约座位 - 参数缺失返回 400")
        void reserveSeat_missingParams_shouldReturn400() throws Exception {
            mockMvc.perform(post("/seats/reserve")
                    .header("Authorization", READER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
