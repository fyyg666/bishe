package com.library.system.controller;

import com.library.system.dto.BookResponse;
import com.library.system.entity.Book;
import com.library.system.entity.BookCategory;
import com.library.system.entity.Seat;
import com.library.system.entity.User;
import com.library.system.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 统计控制器集成测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookMapper bookMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private BorrowRecordMapper borrowRecordMapper;

    @MockBean
    private SeatMapper seatMapper;

    @MockBean
    private SeatReservationMapper seatReservationMapper;

    @MockBean
    private BookCategoryMapper bookCategoryMapper;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetOverview_Success() throws Exception {
        when(borrowRecordMapper.selectCount(any())).thenReturn(100L);
        when(borrowRecordMapper.selectCount(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(80L);
        when(bookMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(bookCategoryMapper.selectCount(any())).thenReturn(10L);
        when(userMapper.selectCount(any())).thenReturn(50L);
        when(seatMapper.selectCount(any())).thenReturn(100L);

        mockMvc.perform(get("/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetBorrowStats_Success() throws Exception {
        when(borrowRecordMapper.selectCount(any())).thenReturn(100L);
        when(bookMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(bookCategoryMapper.selectCount(any())).thenReturn(10L);
        when(userMapper.selectCount(any())).thenReturn(50L);
        when(seatMapper.selectCount(any())).thenReturn(100L);

        mockMvc.perform(get("/statistics/borrows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetBookStats_Success() throws Exception {
        Book book = new Book();
        book.setTotalCount(5);
        book.setAvailableCount(3);
        
        when(bookMapper.selectList(any())).thenReturn(Arrays.asList(book));
        when(bookCategoryMapper.selectCount(any())).thenReturn(10L);

        mockMvc.perform(get("/statistics/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetReaderStats_Success() throws Exception {
        User reader = new User();
        reader.setCreditScore(100);
        
        when(userMapper.selectCount(any())).thenReturn(50L);
        when(borrowRecordMapper.selectCount(any())).thenReturn(10L);
        when(userMapper.selectList(any())).thenReturn(Arrays.asList(reader));

        mockMvc.perform(get("/statistics/readers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetSeatStats_Success() throws Exception {
        when(seatMapper.selectCount(any())).thenReturn(100L);
        when(seatReservationMapper.selectCount(any())).thenReturn(30L);

        mockMvc.perform(get("/statistics/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetBorrowTrend_Success() throws Exception {
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        mockMvc.perform(get("/statistics/borrow-trend")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetBorrowTrend_CustomDays() throws Exception {
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        mockMvc.perform(get("/statistics/borrow-trend")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetHotBooks_Success() throws Exception {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java编程思想");
        book.setAuthor("Bruce Eckel");
        book.setBorrowCount(100);

        when(bookMapper.selectHotBooks(10)).thenReturn(Arrays.asList(book));

        mockMvc.perform(get("/statistics/hot-books")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetCategoryDistribution_Success() throws Exception {
        Book book = new Book();
        book.setCategoryId(1L);
        
        BookCategory category = new BookCategory();
        category.setId(1L);
        category.setName("编程");

        when(bookMapper.selectList(any())).thenReturn(Arrays.asList(book));
        when(bookCategoryMapper.selectList(any())).thenReturn(Arrays.asList(category));

        mockMvc.perform(get("/statistics/category-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetMonthlyStats_Success() throws Exception {
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectCount(any())).thenReturn(0L);

        mockMvc.perform(get("/statistics/monthly")
                        .param("months", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetOverview_Forbidden() throws Exception {
        mockMvc.perform(get("/statistics/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetOverview_Unauthorized() throws Exception {
        mockMvc.perform(get("/statistics/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetOverview_AdminAccess() throws Exception {
        when(borrowRecordMapper.selectCount(any())).thenReturn(100L);
        when(bookMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(bookCategoryMapper.selectCount(any())).thenReturn(10L);
        when(userMapper.selectCount(any())).thenReturn(50L);
        when(seatMapper.selectCount(any())).thenReturn(100L);

        mockMvc.perform(get("/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
