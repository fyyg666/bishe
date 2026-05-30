package com.library.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 座位预约请求DTO 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservationRequest {

    /**
     * 座位ID
     */
    private Long seatId;

    /**
     * 座位编号
     */
    private String seatNumber;

    /**
     * 阅览室ID
     */
    private Long roomId;

    /**
     * 预约日期
     */
    @NotNull(message = "预约日期不能为空")
    private LocalDate reservationDate;

    /**
     * 区域名称
     */
    private String area;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    /**
     * 预约来源
     */
    private String source;

    /**
     * 获取预约日期字符串
     */
    public String getReservationDateStr() {
        if (reservationDate == null) return null;
        return reservationDate.toString();
    }

    /**
     * 获取开始时间字符串
     */
    public String getStartTimeStr() {
        if (startTime == null) return null;
        return startTime.toString();
    }

    /**
     * 获取结束时间字符串
     */
    public String getEndTimeStr() {
        if (endTime == null) return null;
        return endTime.toString();
    }
}
