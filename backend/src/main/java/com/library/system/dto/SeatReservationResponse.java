package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 座位预约响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservationResponse {

    /**
     * 预约ID
     */
    private Long id;

    /**
     * 座位编号
     */
    private String seatNumber;

    /**
     * 区域名称
     */
    private String area;

    /**
     * 用户名
     */
    private String username;

    /**
     * 预约日期
     */
    private LocalDate reservationDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 预约状态
     */
    private String status;

    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 签退时间
     */
    private LocalDateTime checkOutTime;

    /**
     * 预约来源
     */
    private String source;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
