package com.library.system.controller;

import com.library.system.dto.*;
import com.library.system.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 座位控制器
 * <p>
 * 处理座位预约、签到、签退等操作，以及座位可用性检查和预约记录查询。
 * 预约/签到/签退操作需要READER角色权限。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
@Tag(name = "座位管理", description = "座位预约、签到、签退和可用性查询")
@SecurityRequirement(name = "bearerAuth")
public class SeatController extends BaseController { // FIXED: ARCH-002 继承BaseController

    private final SeatService seatService;

    @Operation(summary = "获取座位详情", description = "根据ID查询座位详细信息及当日预约状态")
    @GetMapping("/{id}")
    public ApiResponse<SeatDetailResponse> getSeatDetail(
            @Parameter(description = "座位ID", required = true) @PathVariable Long id) {
        log.debug("查询座位详情: id={}", id);
        SeatDetailResponse detail = seatService.getSeatDetail(id);
        return ApiResponse.success(detail);
    }

    /**
     * 获取座位列表
     */
    @Operation(summary = "获取座位列表", description = "查询座位列表及可用性状态")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    public ApiResponse<List<SeatReservationResponse>> listSeats(
            @Parameter(description = "区域名称（可选）")
            @RequestParam(required = false) String area,
            @Parameter(description = "日期（默认今天）")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        log.debug("查询座位列表: area={}, date={}", area, date);
        List<SeatReservationResponse> seats = seatService.listSeats(area, date);
        return ApiResponse.success(seats);
    }

    /**
     * 预约座位
     */
    @Operation(summary = "预约座位", description = "预约图书馆座位（需要READER角色）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "预约成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权预约"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "时间段冲突")
    })
    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<SeatReservationResponse> reserveSeat(
            @Parameter(description = "预约请求体", required = true)
            @Valid @RequestBody SeatReservationRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("预约座位: userId={}, seat={}, date={}", userId, request.getSeatNumber(), request.getReservationDate());
        SeatReservationResponse response = seatService.reserveSeat(userId, request);
        return ApiResponse.success("预约成功", response);
    }

    /**
     * 取消预约
     */
    @Operation(summary = "取消预约", description = "取消已预约的座位")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权取消"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "预约记录不存在")
    })
    @PostMapping("/cancel/{reservationId}")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> cancelReservation(
            @Parameter(description = "预约记录ID", required = true)
            @PathVariable Long reservationId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("取消预约: userId={}, reservationId={}", userId, reservationId);
        seatService.cancelReservation(userId, reservationId);
        return ApiResponse.success("取消预约成功", null);
    }

    /**
     * 签到
     */
    @Operation(summary = "座位签到", description = "到达座位后扫码签到")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "签到成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "签到失败（如未到时间）"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权签到")
    })
    @PostMapping("/checkin/{reservationId}")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<SeatReservationResponse> checkIn(
            @Parameter(description = "预约记录ID", required = true)
            @PathVariable Long reservationId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("座位签到: userId={}, reservationId={}", userId, reservationId);
        SeatReservationResponse response = seatService.checkIn(userId, reservationId);
        return ApiResponse.success("签到成功", response);
    }

    /**
     * 签退
     */
    @Operation(summary = "座位签退", description = "离开座位时签退")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "签退成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权签退")
    })
    @PostMapping("/checkout/{reservationId}")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<SeatReservationResponse> checkOut(
            @Parameter(description = "预约记录ID", required = true)
            @PathVariable Long reservationId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("座位签退: userId={}, reservationId={}", userId, reservationId);
        SeatReservationResponse response = seatService.checkOut(userId, reservationId);
        return ApiResponse.success("签退成功", response);
    }

    /**
     * 获取我的预约列表
     */
    @Operation(summary = "获取我的预约列表", description = "分页查询当前用户的座位预约记录")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<SeatReservationResponse>> getMyReservations(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.debug("查询我的预约: userId={}, current={}, size={}", userId, current, size);
        PageResult<SeatReservationResponse> result = seatService.getMyReservations(userId, current, size);
        return ApiResponse.success(result);
    }

    /**
     * 检查时间段是否可用
     */
    @Operation(summary = "检查座位可用性", description = "检查指定时间段座位是否可用")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查成功")
    })
    @GetMapping("/check-availability")
    public ApiResponse<Boolean> checkAvailability(
            @Parameter(description = "座位编号", required = true)
            @RequestParam String seatNumber,
            @Parameter(description = "日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "开始时间", required = true)
            @RequestParam String startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam String endTime) {
        log.debug("检查座位可用性: seat={}, date={} {}-{}", seatNumber, date, startTime, endTime);
        boolean available = seatService.isTimeSlotAvailable(seatNumber, date, startTime, endTime);
        return ApiResponse.success(available);
    }
}
