package com.library.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.BookRequest;
import com.library.system.dto.BookResponse;
import com.library.system.dto.PageResult;
import com.library.system.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 图书控制器集成测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    private BookResponse testBookResponse;
    private BookRequest testBookRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试图书响应
        testBookResponse = BookResponse.builder()
                .id(1L)
                .isbn("9787111213826")
                .title("Java编程思想")
                .author("Bruce Eckel")
                .publisher("机械工业出版社")
                .publishDate("2007-06")
                .categoryId(1L)
                .categoryName("编程")
                .description("Java经典书籍")
                .location("A-101")
                .totalCount(5)
                .availableCount(3)
                .price(new BigDecimal("108.00"))
                .borrowCount(2)
                .status(1)
                .build();

        // 初始化测试图书请求
        testBookRequest = BookRequest.builder()
                .isbn("9787111213826")
                .title("Java编程思想")
                .author("Bruce Eckel")
                .publisher("机械工业出版社")
                .publishDate("2007-06")
                .categoryId(1L)
                .description("Java经典书籍")
                .location("A-101")
                .totalCount(5)
                .price(new BigDecimal("108.00"))
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testListBooks_Success() throws Exception {
        List<BookResponse> books = Arrays.asList(testBookResponse);
        PageResult<BookResponse> pageResult = PageResult.of(1L, 10L, 1L, books);

        when(bookService.listBooks(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/books")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].title").value("Java编程思想"))
                .andExpect(jsonPath("$.data.records[0].isbn").value("9787111213826"));

        verify(bookService).listBooks(1L, 10L, "Java", null);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testListBooks_WithCategoryFilter() throws Exception {
        List<BookResponse> books = Arrays.asList(testBookResponse);
        PageResult<BookResponse> pageResult = PageResult.of(1L, 10L, 1L, books);

        when(bookService.listBooks(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/books")
                        .param("current", "1")
                        .param("size", "10")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(bookService).listBooks(1L, 10L, null, 1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetBookById_Success() throws Exception {
        when(bookService.getBookById(1L)).thenReturn(testBookResponse);

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Java编程思想"))
                .andExpect(jsonPath("$.data.author").value("Bruce Eckel"));

        verify(bookService).getBookById(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetBookById_NotFound() throws Exception {
        when(bookService.getBookById(999L))
                .thenThrow(new RuntimeException("图书不存在"));

        mockMvc.perform(get("/books/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(bookService).getBookById(999L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetHotBooks_Success() throws Exception {
        List<BookResponse> books = Arrays.asList(testBookResponse);

        when(bookService.getHotBooks(10)).thenReturn(books);

        mockMvc.perform(get("/books/hot")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("Java编程思想"));

        verify(bookService).getHotBooks(10);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_Success() throws Exception {
        when(bookService.createBook(any(BookRequest.class))).thenReturn(testBookResponse);

        mockMvc.perform(post("/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Java编程思想"));

        verify(bookService).createBook(any(BookRequest.class));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_ValidationError() throws Exception {
        BookRequest invalidRequest = BookRequest.builder()
                .isbn("")  // ISBN为空
                .title("")  // 标题为空
                .build();

        mockMvc.perform(post("/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateBook_Forbidden() throws Exception {
        mockMvc.perform(post("/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBookRequest)))
                .andExpect(status().isForbidden());

        verify(bookService, never()).createBook(any());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateBook_Success() throws Exception {
        BookRequest updateRequest = BookRequest.builder()
                .isbn("9787111213826")
                .title("Java编程思想（第5版）")
                .author("Bruce Eckel")
                .publisher("机械工业出版社")
                .publishDate("2007-06")
                .categoryId(1L)
                .totalCount(6)
                .build();

        BookResponse updatedResponse = BookResponse.builder()
                .id(1L)
                .isbn("9787111213826")
                .title("Java编程思想（第5版）")
                .author("Bruce Eckel")
                .totalCount(6)
                .availableCount(4)
                .build();

        when(bookService.updateBook(eq(1L), any(BookRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/books/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Java编程思想（第5版）"));

        verify(bookService).updateBook(eq(1L), any(BookRequest.class));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteBook_Success() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/books/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("图书删除成功"));

        verify(bookService).deleteBook(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteBook_Forbidden() throws Exception {
        mockMvc.perform(delete("/books/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(bookService, never()).deleteBook(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCheckIsbn_Exists() throws Exception {
        when(bookService.isIsbnExists("9787111213826")).thenReturn(true);

        mockMvc.perform(get("/books/check-isbn")
                        .param("isbn", "9787111213826"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCheckIsbn_NotExists() throws Exception {
        when(bookService.isIsbnExists("9787111000000")).thenReturn(false);

        mockMvc.perform(get("/books/check-isbn")
                        .param("isbn", "9787111000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void testListBooks_Unauthorized() throws Exception {
        mockMvc.perform(get("/books")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateBook_AdminSuccess() throws Exception {
        when(bookService.createBook(any(BookRequest.class))).thenReturn(testBookResponse);

        mockMvc.perform(post("/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(bookService).createBook(any(BookRequest.class));
    }
}
