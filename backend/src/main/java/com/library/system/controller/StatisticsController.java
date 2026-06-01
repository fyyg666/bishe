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

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
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
public class StatisticsController extends BaseController {

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

    @Operation(summary = "导出统计报表Excel", description = "导出统计报表为Excel文件（需要管理员权限）")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public void exportStatistics(
            @Parameter(description = "报表类型: overview/borrows/books/readers/seats")
            @RequestParam(defaultValue = "overview") String type,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("统计报表_" + type + "_" + LocalDate.now(), "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ExcelWriter excelWriter = EasyExcel
                .write(response.getOutputStream())
                .autoCloseStream(false)
                .build();

        try {
            List<StatisticsExportDTO.OverviewExport> overviewData = buildOverviewData();
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "综合概览")
                    .head(StatisticsExportDTO.OverviewExport.class).build();
            excelWriter.write(overviewData, sheet1);

            List<StatisticsExportDTO.BorrowTrendExport> trendData = buildBorrowTrendData();
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "借阅趋势")
                    .head(StatisticsExportDTO.BorrowTrendExport.class).build();
            excelWriter.write(trendData, sheet2);

            List<StatisticsExportDTO.CategoryDistributionExport> categoryData = buildCategoryData();
            WriteSheet sheet3 = EasyExcel.writerSheet(2, "分类分布")
                    .head(StatisticsExportDTO.CategoryDistributionExport.class).build();
            excelWriter.write(categoryData, sheet3);
        } finally {
            excelWriter.finish();
        }
    }

    private List<StatisticsExportDTO.OverviewExport> buildOverviewData() {
        StatisticsResponse overview = statisticsService.getOverview();
        List<StatisticsExportDTO.OverviewExport> list = new ArrayList<>();
        if (overview != null) {
            StatisticsResponse.BookStatistics bookStats = overview.getBookStatistics();
            if (bookStats != null) {
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("图书总数").value(bookStats.getTotalBooks() != null ? bookStats.getTotalBooks() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("图书总副本数").value(bookStats.getTotalCopies() != null ? bookStats.getTotalCopies() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("可借副本数").value(bookStats.getAvailableCopies() != null ? bookStats.getAvailableCopies() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("已借副本数").value(bookStats.getBorrowedCopies() != null ? bookStats.getBorrowedCopies() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("分类数").value(bookStats.getCategories() != null ? bookStats.getCategories() : 0L).build());
            }
            StatisticsResponse.ReaderStatistics readerStats = overview.getReaderStatistics();
            if (readerStats != null) {
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("读者总数").value(readerStats.getTotalReaders() != null ? readerStats.getTotalReaders() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("活跃读者").value(readerStats.getActiveReaders() != null ? readerStats.getActiveReaders() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("逾期读者").value(readerStats.getOverdueReaders() != null ? readerStats.getOverdueReaders() : 0L).build());
            }
            StatisticsResponse.BorrowStatistics borrowStats = overview.getBorrowStatistics();
            if (borrowStats != null) {
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("借阅总数").value(borrowStats.getTotalBorrows() != null ? borrowStats.getTotalBorrows() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("借阅中").value(borrowStats.getActiveBorrows() != null ? borrowStats.getActiveBorrows() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("逾期借阅").value(borrowStats.getOverdueBorrows() != null ? borrowStats.getOverdueBorrows() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("今日归还").value(borrowStats.getReturnedToday() != null ? borrowStats.getReturnedToday() : 0L).build());
            }
            StatisticsResponse.SeatStatistics seatStats = overview.getSeatStatistics();
            if (seatStats != null) {
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("座位总数").value(seatStats.getTotalSeats() != null ? seatStats.getTotalSeats() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("可用座位").value(seatStats.getAvailableSeats() != null ? seatStats.getAvailableSeats() : 0L).build());
                list.add(StatisticsExportDTO.OverviewExport.builder().metric("今日预约").value(seatStats.getTodayReservations() != null ? seatStats.getTodayReservations() : 0L).build());
            }
        }
        return list;
    }

    private List<StatisticsExportDTO.BorrowTrendExport> buildBorrowTrendData() {
        List<Map<String, Object>> trend = statisticsService.getBorrowTrend(30);
        List<StatisticsExportDTO.BorrowTrendExport> list = new ArrayList<>();
        if (trend != null) {
            for (Map<String, Object> item : trend) {
                list.add(StatisticsExportDTO.BorrowTrendExport.builder()
                        .date(String.valueOf(item.get("date")))
                        .borrowCount(item.get("borrowCount") != null ? ((Number) item.get("borrowCount")).longValue() : 0L)
                        .returnCount(item.get("returnCount") != null ? ((Number) item.get("returnCount")).longValue() : 0L)
                        .build());
            }
        }
        return list;
    }

    private List<StatisticsExportDTO.CategoryDistributionExport> buildCategoryData() {
        List<Map<String, Object>> dist = statisticsService.getCategoryDistribution();
        List<StatisticsExportDTO.CategoryDistributionExport> list = new ArrayList<>();
        if (dist != null) {
            for (Map<String, Object> item : dist) {
                list.add(StatisticsExportDTO.CategoryDistributionExport.builder()
                        .categoryName(String.valueOf(item.get("name")))
                        .bookCount(item.get("bookCount") != null ? ((Number) item.get("bookCount")).longValue() : 0L)
                        .borrowCount(item.get("borrowCount") != null ? ((Number) item.get("borrowCount")).longValue() : 0L)
                        .build());
            }
        }
        return list;
    }
}
