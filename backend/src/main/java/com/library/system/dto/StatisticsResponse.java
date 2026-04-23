package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    /**
     * 借阅统计
     */
    private BorrowStatistics borrowStatistics;

    /**
     * 图书统计
     */
    private BookStatistics bookStatistics;

    /**
     * 读者统计
     */
    private ReaderStatistics readerStatistics;

    /**
     * 座位统计
     */
    private SeatStatistics seatStatistics;

    /**
     * 借阅趋势数据
     */
    private List<Map<String, Object>> borrowTrend;

    /**
     * 热门图书数据
     */
    private List<BookResponse> hotBooks;

    /**
     * 月度统计数据
     */
    private List<Map<String, Object>> monthlyStats;

    /**
     * 借阅统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BorrowStatistics {
        private Long totalBorrows;
        private Long activeBorrows;
        private Long overdueBorrows;
        private Long returnedToday;
        private Double averageBorrowDays;
    }

    /**
     * 图书统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookStatistics {
        private Long totalBooks;
        private Long totalCopies;
        private Long availableCopies;
        private Long borrowedCopies;
        private Long categories;
    }

    /**
     * 读者统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReaderStatistics {
        private Long totalReaders;
        private Long activeReaders;
        private Long overdueReaders;
        private Double averageCreditScore;
    }

    /**
     * 座位统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatStatistics {
        private Long totalSeats;
        private Long availableSeats;
        private Long occupiedSeats;
        private Long todayReservations;
    }
}
