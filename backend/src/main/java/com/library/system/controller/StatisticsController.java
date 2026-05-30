package com.library.system.controller;

import com.library.system.dto.*;
import com.library.system.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计分析控制器
 * <p>
 * 提供系统各类统计数据的查询，业务逻辑委托给 {@link StatisticsService}。
 * 所有接口需要ADMIN或LIBRARIAN角色权限（热门图书除外）。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "系统各类统计数据的查询和展示")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取综合概览
     */
    @Operation(summary = "获取综合概览", description = "获取系统综合统计数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = StatisticsResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<StatisticsResponse> getOverview() {
        log.debug("获取综合统计概览");
        return ApiResponse.success(statisticsService.getOverview());
    }

    /**
     * 获取借阅统计
     */
    @Operation(summary = "获取借阅统计", description = "获取借阅相关统计数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/borrows")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<StatisticsResponse.BorrowStatistics> getBorrowStats() {
        log.debug("获取借阅统计");
        return ApiResponse.success(statisticsService.getBorrowStatistics());
    }

    /**
     * 获取图书统计
     */
    @Operation(summary = "获取图书统计", description = "获取图书相关统计数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/books")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<StatisticsResponse.BookStatistics> getBookStats() {
        log.debug("获取图书统计");
        return ApiResponse.success(statisticsService.getBookStatistics());
    }

    /**
     * 获取读者统计
     */
    @Operation(summary = "获取读者统计", description = "获取读者相关统计数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/readers")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<StatisticsResponse.ReaderStatistics> getReaderStats() {
        log.debug("获取读者统计");
        return ApiResponse.success(statisticsService.getReaderStatistics());
    }

    /**
     * 获取座位统计
     */
    @Operation(summary = "获取座位统计", description = "获取座位相关统计数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/seats")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<StatisticsResponse.SeatStatistics> getSeatStats() {
        log.debug("获取座位统计");
        return ApiResponse.success(statisticsService.getSeatStatistics());
    }

    /**
     * 获取借阅趋势
     */
    @Operation(summary = "获取借阅趋势", description = "获取指定天数内的借阅趋势数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/borrow-trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<Map<String, Object>>> getBorrowTrend(
            @Parameter(description = "统计天数（默认30天）")
            @RequestParam(defaultValue = "30") Integer days) {
        log.debug("获取借阅趋势: days={}", days);
        return ApiResponse.success(statisticsService.getBorrowTrend(days));
    }

    /**
     * 获取热门图书
     */
    @Operation(summary = "获取热门图书", description = "获取热门借阅图书排行榜（公开接口）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/hot-books")
    public ApiResponse<List<BookResponse>> getHotBooks(
            @Parameter(description = "返回数量（默认10）")
            @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("获取热门图书: limit={}", limit);
        return ApiResponse.success(statisticsService.getHotBooks(limit));
    }

    /**
     * 获取图书分类分布
     */
    @Operation(summary = "获取图书分类分布", description = "获取各类图书的数量分布（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/category-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<Map<String, Object>>> getCategoryDistribution() {
        log.debug("获取图书分类分布");
        return ApiResponse.success(statisticsService.getCategoryDistribution());
    }

    /**
     * 获取座位使用率热力图
     */
    @Operation(summary = "获取座位使用率热力图", description = "获取各时间段各区域的座位使用率数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/seat-heatmap")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<Map<String, Object>>> getSeatHeatmap() {
        log.debug("获取座位使用率热力图");
        return ApiResponse.success(statisticsService.getSeatHeatmap());
    }

    /**
     * 获取月度统计
     */
    @Operation(summary = "获取月度统计", description = "获取指定月数内的月度统计数据（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<Map<String, Object>>> getMonthlyStats(
            @Parameter(description = "统计月数（默认12个月）")
            @RequestParam(defaultValue = "12") Integer months) {
        log.debug("获取月度统计: months={}", months);
        return ApiResponse.success(statisticsService.getMonthlyStats(months));
    }
}
