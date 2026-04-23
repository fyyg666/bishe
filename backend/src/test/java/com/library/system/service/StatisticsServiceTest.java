package com.library.system.service;

import com.library.system.dto.BorrowTrendDTO;
import com.library.system.entity.Book;
import com.library.system.entity.Borrow;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BorrowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 统计服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private BorrowMapper borrowMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private Borrow testBorrow;
    private Book testBook;

    @BeforeEach
    void setUp() {
        testBorrow = new Borrow();
        testBorrow.setId(1L);
        testBorrow.setBookId(1L);
        testBorrow.setReaderId(1L);
        testBorrow.setBorrowDate(LocalDateTime.now().minusDays(10));
        testBorrow.setDueDate(LocalDateTime.now().plusDays(20));
        testBorrow.setStatus(1);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("9787111213826");
        testBook.setTitle("Java编程思想");
        testBook.setAuthor("Bruce Eckel");
        testBook.setCategoryId(1L);
        testBook.setTotalCount(5);
        testBook.setAvailableCount(3);
        testBook.setBorrowCount(2);
    }

    @Test
    void testGetBorrowTrend_Success() {
        List<Object> trendData = Arrays.asList(
                new Object[]{"2024-01", 10L},
                new Object[]{"2024-02", 15L},
                new Object[]{"2024-03", 20L}
        );
        when(borrowMapper.selectBorrowTrend(anyInt())).thenReturn(trendData);

        List<BorrowTrendDTO> result = statisticsService.getBorrowTrend(6);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(borrowMapper).selectBorrowTrend(6);
    }

    @Test
    void testGetBorrowTrend_EmptyResult() {
        when(borrowMapper.selectBorrowTrend(anyInt())).thenReturn(Arrays.asList());

        List<BorrowTrendDTO> result = statisticsService.getBorrowTrend(6);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(borrowMapper).selectBorrowTrend(6);
    }

    @Test
    void testGetHotBooks_Success() {
        List<Book> hotBooks = Arrays.asList(testBook);
        when(bookMapper.selectHotBooks(anyInt())).thenReturn(hotBooks);

        List<Book> result = statisticsService.getHotBooks(10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Java编程思想", result.get(0).getTitle());
        verify(bookMapper).selectHotBooks(10);
    }

    @Test
    void testGetOverdueStats_Success() {
        List<Object> overdueStats = Arrays.asList(
                new Object[]{1L, "张三", 2L},
                new Object[]{2L, "李四", 1L}
        );
        when(borrowMapper.selectOverdueStats()).thenReturn(overdueStats);

        List<Object> result = statisticsService.getOverdueStats();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(borrowMapper).selectOverdueStats();
    }

    @Test
    void testGetCategoryStats_Success() {
        List<Object> categoryStats = Arrays.asList(
                new Object[]{1L, "计算机", 50L},
                new Object[]{2L, "文学", 30L}
        );
        when(bookMapper.selectCategoryStats()).thenReturn(categoryStats);

        List<Object> result = statisticsService.getCategoryStats();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(bookMapper).selectCategoryStats();
    }

    @Test
    void testGetDailyBorrowStats_Success() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        List<Object> dailyStats = Arrays.asList(
                new Object[]{LocalDate.now().minusDays(6), 5L},
                new Object[]{LocalDate.now().minusDays(5), 8L}
        );
        when(borrowMapper.selectDailyBorrowStats(any(), any())).thenReturn(dailyStats);

        List<Object> result = statisticsService.getDailyBorrowStats(startDate, endDate);

        assertNotNull(result);
        verify(borrowMapper).selectDailyBorrowStats(any(), any());
    }

    @Test
    void testGetReaderBorrowRank_Success() {
        List<Object> readerRank = Arrays.asList(
                new Object[]{1L, "张三", 15L},
                new Object[]{2L, "李四", 12L}
        );
        when(borrowMapper.selectReaderBorrowRank(anyInt())).thenReturn(readerRank);

        List<Object> result = statisticsService.getReaderBorrowRank(10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(borrowMapper).selectReaderBorrowRank(10);
    }

    @Test
    void testGetBookBorrowRate_Success() {
        List<Object> borrowRate = Arrays.asList(
                new Object[]{1L, "Java编程思想", 20L}
        );
        when(borrowMapper.selectBookBorrowRate(anyInt())).thenReturn(borrowRate);

        List<Object> result = statisticsService.getBookBorrowRate(10);

        assertNotNull(result);
        verify(borrowMapper).selectBookBorrowRate(10);
    }

    @Test
    void testGetLibraryUsageStats_Success() {
        // Mock multiple queries
        when(borrowMapper.selectCount(any())).thenReturn(100L);
        when(bookMapper.selectCount(any())).thenReturn(500L);

        Object result = statisticsService.getLibraryUsageStats();

        assertNotNull(result);
        verify(borrowMapper, atLeastOnce()).selectCount(any());
        verify(bookMapper, atLeastOnce()).selectCount(any());
    }

    @Test
    void testGetBorrowTrend_InvalidMonths() {
        // Test with invalid months parameter
        assertThrows(IllegalArgumentException.class, () -> 
                statisticsService.getBorrowTrend(-1));
        
        assertThrows(IllegalArgumentException.class, () -> 
                statisticsService.getBorrowTrend(0));
    }

    @Test
    void testGetHotBooks_InvalidLimit() {
        // Test with invalid limit parameter
        assertThrows(IllegalArgumentException.class, () -> 
                statisticsService.getHotBooks(-1));
    }

    @Test
    void testGetDailyBorrowStats_InvalidDateRange() {
        // Test with end date before start date
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> 
                statisticsService.getDailyBorrowStats(startDate, endDate));
    }
}
