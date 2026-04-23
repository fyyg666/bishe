package com.library.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.BorrowRequest;
import com.library.system.dto.BorrowResponse;
import com.library.system.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 借阅控制器单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(BorrowController.class)
class BorrowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BorrowService borrowService;

    private BorrowRequest validBorrowRequest;
    private BorrowResponse testBorrowResponse;

    @BeforeEach
    void setUp() {
        validBorrowRequest = BorrowRequest.builder()
                .bookId(1L)
                .readerId(1L)
                .build();

        testBorrowResponse = BorrowResponse.builder()
                .id(1L)
                .bookId(1L)
                .bookTitle("Java编程思想")
                .readerId(1L)
                .readerName("张三")
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(30))
                .status(1)
                .build();
    }

    @Test
    void testBorrowBook_Success() throws Exception {
        when(borrowService.borrowBook(any(BorrowRequest.class))).thenReturn(testBorrowResponse);

        mockMvc.perform(post("/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBorrowRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("借书成功"))
                .andExpect(jsonPath("$.data.bookTitle").value("Java编程思想"))
                .andExpect(jsonPath("$.data.status").value(1));

        verify(borrowService).borrowBook(any(BorrowRequest.class));
    }

    @Test
    void testBorrowBook_BookNotFound() throws Exception {
        when(borrowService.borrowBook(any(BorrowRequest.class)))
                .thenThrow(new RuntimeException("图书不存在"));

        mockMvc.perform(post("/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBorrowRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("图书不存在"));

        verify(borrowService).borrowBook(any(BorrowRequest.class));
    }

    @Test
    void testBorrowBook_NoAvailableCopies() throws Exception {
        when(borrowService.borrowBook(any(BorrowRequest.class)))
                .thenThrow(new RuntimeException("库存不足"));

        mockMvc.perform(post("/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBorrowRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    void testBorrowBook_ReaderNotFound() throws Exception {
        when(borrowService.borrowBook(any(BorrowRequest.class)))
                .thenThrow(new RuntimeException("读者不存在"));

        mockMvc.perform(post("/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBorrowRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testBorrowBook_ExceedMaxBorrowLimit() throws Exception {
        when(borrowService.borrowBook(any(BorrowRequest.class)))
                .thenThrow(new RuntimeException("超过最大借阅数量限制"));

        mockMvc.perform(post("/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBorrowRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("超过最大借阅数量限制"));
    }

    @Test
    void testReturnBook_Success() throws Exception {
        doNothing().when(borrowService).returnBook(anyLong(), anyLong());

        mockMvc.perform(post("/borrows/1/return")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("还书成功"));

        verify(borrowService).returnBook(1L, 1L);
    }

    @Test
    void testReturnBook_BorrowNotFound() throws Exception {
        doThrow(new RuntimeException("借阅记录不存在"))
                .when(borrowService).returnBook(anyLong(), anyLong());

        mockMvc.perform(post("/borrows/999/return")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("借阅记录不存在"));
    }

    @Test
    void testGetMyBorrows_Success() throws Exception {
        List<BorrowResponse> borrowList = Arrays.asList(testBorrowResponse);
        when(borrowService.getBorrowsByReader(anyLong())).thenReturn(borrowList);

        mockMvc.perform(get("/borrows/my")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].bookTitle").value("Java编程思想"));

        verify(borrowService).getBorrowsByReader(1L);
    }

    @Test
    void testGetOverdueBorrows_Success() throws Exception {
        List<BorrowResponse> overdueList = Arrays.asList(testBorrowResponse);
        when(borrowService.getOverdueBorrows()).thenReturn(overdueList);

        mockMvc.perform(get("/borrows/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(borrowService).getOverdueBorrows();
    }

    @Test
    void testRenewBook_Success() throws Exception {
        doNothing().when(borrowService).renewBook(anyLong(), anyLong());

        mockMvc.perform(post("/borrows/1/renew")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("续借成功"));

        verify(borrowService).renewBook(1L, 1L);
    }

    @Test
    void testBorrowBook_ValidationError_InvalidBookId() throws Exception {
        BorrowRequest invalidRequest = BorrowRequest.builder()
                .bookId(0L)  // Invalid ID
                .readerId(1L)
                .build();

        // This should trigger validation error
        mockMvc.perform(post("/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
