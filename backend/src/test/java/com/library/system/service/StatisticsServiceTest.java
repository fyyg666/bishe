package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.common.Constants;
import com.library.system.dto.BookResponse;
import com.library.system.dto.StatisticsResponse;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.Seat;
import com.library.system.entity.SeatReservation;
import com.library.system.entity.User;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BorrowRecordMapper;
import com.library.system.mapper.SeatMapper;
import com.library.system.mapper.SeatReservationMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("StatisticsService 单元测试")
class StatisticsServiceTest extends BaseTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private SeatMapper seatMapper;

    @Mock
    private SeatReservationMapper seatReservationMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Nested
    @DisplayName("概览统计用例")
    class OverviewTests {

        @Test
        @DisplayName("getOverview - 应聚合返回所有子统计")
        void getOverview_shouldAggregateAllSubStatistics() {
            // Arrange
            when(borrowRecordMapper.selectCount(any())).thenReturn(100L, 50L, 10L, 5L);
            when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyString()))
                    .thenReturn(14.5);
            // Reader 统计中的积分 AVG 查询
            when(jdbcTemplate.queryForObject(contains("AVG(credit_score)"), eq(Double.class), any(), anyString(), anyString()))
                    .thenReturn(85.3);
            when(jdbcTemplate.queryForMap(anyString())).thenReturn(Map.of(
                    "total_books", 100L, "total_copies", 500L,
                    "available_copies", 350L, "categories", 10L));
            when(userMapper.selectCount(any())).thenReturn(200L, 60L, 20L);
            when(seatMapper.selectCount(any())).thenReturn(50L, 30L);
            when(seatReservationMapper.selectCount(any())).thenReturn(15L);

            // Act
            StatisticsResponse result = statisticsService.getOverview();

            // Assert
            assertNotNull(result);
            assertNotNull(result.getBorrowStatistics());
            assertNotNull(result.getBookStatistics());
            assertNotNull(result.getReaderStatistics());
            assertNotNull(result.getSeatStatistics());
            verify(borrowRecordMapper, atLeast(4)).selectCount(any());
        }
    }

    @Nested
    @DisplayName("借阅统计用例")
    class BorrowStatisticsTests {

        @Test
        @DisplayName("getBorrowStatistics - 应正确统计各借阅指标")
        void getBorrowStatistics_shouldReturnCorrectStats() {
            // Arrange
            when(borrowRecordMapper.selectCount(any())).thenReturn(100L, 50L, 10L, 5L);
            when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyString()))
                    .thenReturn(7.5);

            // Act
            StatisticsResponse.BorrowStatistics result = statisticsService.getBorrowStatistics();

            // Assert
            assertEquals(100L, result.getTotalBorrows());
            assertEquals(50L, result.getActiveBorrows());
            assertEquals(10L, result.getOverdueBorrows());
            assertEquals(5L, result.getReturnedToday());
            assertEquals(7.5, result.getAverageBorrowDays(), 0.01);
        }

        @Test
        @DisplayName("getBorrowStatistics - 平均借阅天数为null时应返回默认值")
        void getBorrowStatistics_whenAvgDaysNull_shouldReturnDefault() {
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L, 0L, 0L, 0L);
            when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), anyString()))
                    .thenReturn(null);

            StatisticsResponse.BorrowStatistics result = statisticsService.getBorrowStatistics();

            assertEquals(14.0, result.getAverageBorrowDays(), 0.01);
        }
    }

    @Nested
    @DisplayName("图书统计用例")
    class BookStatisticsTests {

        @Test
        @DisplayName("getBookStatistics - 应正确统计图书指标")
        void getBookStatistics_shouldReturnCorrectStats() {
            // Arrange
            Map<String, Object> mockResult = new HashMap<>();
            mockResult.put("total_books", 200L);
            mockResult.put("total_copies", 1000L);
            mockResult.put("available_copies", 700L);
            mockResult.put("categories", 15L);
            when(jdbcTemplate.queryForMap(anyString())).thenReturn(mockResult);

            // Act
            StatisticsResponse.BookStatistics result = statisticsService.getBookStatistics();

            // Assert
            assertEquals(200L, result.getTotalBooks());
            assertEquals(1000L, result.getTotalCopies());
            assertEquals(700L, result.getAvailableCopies());
            assertEquals(300L, result.getBorrowedCopies());
            assertEquals(15L, result.getCategories());
        }
    }

    @Nested
    @DisplayName("读者统计用例")
    class ReaderStatisticsTests {

        @Test
        @DisplayName("getReaderStatistics - 应正确统计读者指标")
        void getReaderStatistics_shouldReturnCorrectStats() {
            when(userMapper.selectCount(any())).thenReturn(150L, 40L, 15L);
            when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), any(), anyString(), anyString()))
                    .thenReturn(85.3);

            StatisticsResponse.ReaderStatistics result = statisticsService.getReaderStatistics();

            assertEquals(150L, result.getTotalReaders());
            assertEquals(40L, result.getActiveReaders());
            assertEquals(15L, result.getOverdueReaders());
            assertEquals(85.3, result.getAverageCreditScore(), 0.1);
        }
    }

    @Nested
    @DisplayName("座位统计用例")
    class SeatStatisticsTests {

        @Test
        @DisplayName("getSeatStatistics - 应正确统计座位指标")
        void getSeatStatistics_shouldReturnCorrectStats() {
            when(seatMapper.selectCount(any())).thenReturn(80L, 60L);
            when(seatReservationMapper.selectCount(any())).thenReturn(25L);

            StatisticsResponse.SeatStatistics result = statisticsService.getSeatStatistics();

            assertEquals(80L, result.getTotalSeats());
            assertEquals(60L, result.getAvailableSeats());
            assertEquals(20L, result.getOccupiedSeats());
            assertEquals(25L, result.getTodayReservations());
        }
    }

    @Nested
    @DisplayName("借阅趋势用例")
    class BorrowTrendTests {

        @Test
        @DisplayName("getBorrowTrend - days为null时应使用默认30天")
        void getBorrowTrend_whenDaysNull_shouldUseDefault30() {
            when(jdbcTemplate.queryForList(anyString(), anyString(), anyString())).thenReturn(List.of());
            when(jdbcTemplate.queryForList(anyString(), anyString(), anyString(), anyString())).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getBorrowTrend(null);

            assertEquals(30, result.size());
        }

        @Test
        @DisplayName("getBorrowTrend - days为0时应使用默认30天")
        void getBorrowTrend_whenDaysZero_shouldUseDefault30() {
            when(jdbcTemplate.queryForList(anyString(), anyString(), anyString())).thenReturn(List.of());
            when(jdbcTemplate.queryForList(anyString(), anyString(), anyString(), anyString())).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getBorrowTrend(0);

            assertEquals(30, result.size());
        }

        @Test
        @DisplayName("getBorrowTrend - 应正确合并借阅和归还数据")
        void getBorrowTrend_shouldMergeBorrowAndReturnData() {
            List<Map<String, Object>> borrowStats = List.of(
                    Map.of("stat_date", LocalDate.now(), "count", 5L));
            when(jdbcTemplate.queryForList(contains("create_time"), anyString(), anyString())).thenReturn(borrowStats);
            when(jdbcTemplate.queryForList(contains("return_date"), anyString(), anyString(), anyString())).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getBorrowTrend(7);

            assertEquals(7, result.size());
            Map<String, Object> todayData = result.get(result.size() - 1);
            assertNotNull(todayData.get("date"));
            assertNotNull(todayData.get("borrows"));
            assertNotNull(todayData.get("returns"));
        }
    }

    @Nested
    @DisplayName("热门图书用例")
    class HotBooksTests {

        @Test
        @DisplayName("getHotBooks - 应返回热门图书列表")
        void getHotBooks_shouldReturnList() {
            Book book = new Book();
            book.setId(1L);
            book.setTitle("热门图书");
            book.setAuthor("作者");
            book.setIsbn("978-7-111-11111-1");
            book.setPublisher("出版社");
            book.setCategoryId(1L);
            book.setTotalCount(10);
            book.setAvailableCount(8);
            book.setBorrowCount(50);
            book.setPrice(new BigDecimal("59.00"));
            when(bookMapper.selectHotBooks(10)).thenReturn(List.of(book));

            List<BookResponse> result = statisticsService.getHotBooks(10);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("热门图书", result.get(0).getTitle());
            verify(bookMapper).selectHotBooks(10);
        }

        @Test
        @DisplayName("getHotBooks - 无热门图书时应返回空列表")
        void getHotBooks_whenNoData_shouldReturnEmptyList() {
            when(bookMapper.selectHotBooks(anyInt())).thenReturn(List.of());

            List<BookResponse> result = statisticsService.getHotBooks(5);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("分类分布用例")
    class CategoryDistributionTests {

        @Test
        @DisplayName("getCategoryDistribution - 应正确转换分类分布数据")
        void getCategoryDistribution_shouldReturnTransformedData() {
            Map<String, Object> row1 = new HashMap<>();
            row1.put("category_id", 1L);
            row1.put("category_name", "文学");
            row1.put("count", 30L);
            Map<String, Object> row2 = new HashMap<>();
            row2.put("category_id", 2L);
            // 不设置 category_name，模拟 LEFT JOIN 无匹配场景
            row2.put("count", 10L);
            List<Map<String, Object>> rawData = List.of(row1, row2);
            when(jdbcTemplate.queryForList(anyString())).thenReturn(rawData);

            List<Map<String, Object>> result = statisticsService.getCategoryDistribution();

            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).get("categoryId"));
            assertEquals("文学", result.get(0).get("categoryName"));
            assertEquals("未分类", result.get(1).get("categoryName"));
        }

        @Test
        @DisplayName("getCategoryDistribution - 无数据时应返回空列表")
        void getCategoryDistribution_whenNoData_shouldReturnEmptyList() {
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getCategoryDistribution();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("月度统计用例")
    class MonthlyStatsTests {

        @Test
        @DisplayName("getMonthlyStats - months为null时应使用默认12个月")
        void getMonthlyStats_whenMonthsNull_shouldUseDefault12() {
            when(jdbcTemplate.queryForList(anyString(), anyString(), any(), any())).thenReturn(List.of());
            when(jdbcTemplate.queryForList(anyString(), anyString(), any(), any())).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getMonthlyStats(null);

            assertEquals(12, result.size());
            for (Map<String, Object> month : result) {
                assertEquals(0L, month.get("borrows"));
                assertEquals(0L, month.get("returns"));
                assertEquals(0L, month.get("newReaders"));
            }
        }

        @Test
        @DisplayName("getMonthlyStats - 应正确合并借阅/归还/新读者数据")
        void getMonthlyStats_shouldMergeAllData() {
            String currentMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            List<Map<String, Object>> statsData = List.of(
                    Map.of("month", currentMonth, "borrows", 20L, "returns", 15L));
            List<Map<String, Object>> readerData = List.of(
                    Map.of("month", currentMonth, "new_readers", 5L));
            when(jdbcTemplate.queryForList(contains("CASE WHEN 1=1"), anyString(), any(), any())).thenReturn(statsData);
            when(jdbcTemplate.queryForList(contains("new_readers"), anyString(), any(), any())).thenReturn(readerData);

            List<Map<String, Object>> result = statisticsService.getMonthlyStats(1);

            assertEquals(1, result.size());
            assertEquals(20L, result.get(0).get("borrows"));
            assertEquals(15L, result.get(0).get("returns"));
            assertEquals(5L, result.get(0).get("newReaders"));
        }
    }

    @Nested
    @DisplayName("座位热力图用例")
    class SeatHeatmapTests {

        @Test
        @DisplayName("getSeatHeatmap - 应生成完整区域×时间段矩阵")
        void getSeatHeatmap_shouldGenerateFullMatrix() {
            when(jdbcTemplate.queryForList(anyString(), any(LocalDate.class))).thenReturn(List.of());
            when(seatMapper.selectCount(any())).thenReturn(50L);

            List<Map<String, Object>> result = statisticsService.getSeatHeatmap();

            // 3个区域 × 7个时段(8-22每2小时) = 21条
            assertEquals(21, result.size());
            Map<String, Object> first = result.get(0);
            assertNotNull(first.get("area"));
            assertNotNull(first.get("hourSlot"));
            assertNotNull(first.get("reservedCount"));
            assertNotNull(first.get("usageRate"));
            assertEquals(0.0, first.get("usageRate"));
        }

        @Test
        @DisplayName("getSeatHeatmap - 有预约数据时应计算使用率")
        void getSeatHeatmap_withReservationData_shouldCalculateUsageRate() {
            List<Map<String, Object>> rawData = List.of(
                    Map.of("area", "A", "hour_slot", 8, "reserved_count", 5L));
            when(jdbcTemplate.queryForList(anyString(), any(LocalDate.class))).thenReturn(rawData);
            when(seatMapper.selectCount(any())).thenReturn(50L);

            List<Map<String, Object>> result = statisticsService.getSeatHeatmap();

            assertTrue(result.stream().anyMatch(r -> (double) r.get("usageRate") > 0));
        }
    }
}
