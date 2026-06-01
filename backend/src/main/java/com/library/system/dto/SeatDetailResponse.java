package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDetailResponse {

    private Long id;
    private String seatNumber;
    private String status;
    private String location;
    private Long roomId;
    private String roomName;
    private String roomLocation;
    private List<TimeSlot> todayReservations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
        private String status;
        private String username;
    }
}
