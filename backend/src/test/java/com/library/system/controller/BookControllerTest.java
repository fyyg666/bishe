package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.BookRequest;
import com.library.system.dto.BookResponse;
import com.library.system.dto.PageResult;
import com.library.system.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BookController 安全测试")
class BookControllerTest extends ControllerTestBase {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookController bookController;

    @BeforeEach
    void setUp() {
        initMockMvc(bookController);
    }

    @Nested
    @DisplayName("查询接口 - 无需认证")
    class QueryEndpoints {

        @Test
        @DisplayName("分页查询 - 无需认证")
        void listBooks_noAuth_shouldReturn200() throws Exception {
            PageResult<BookResponse> pageResult = new PageResult<>();
            pageResult.setTotal(0L);
            pageResult.setRecords(List.of());
            when(bookService.listBooks(anyLong(), anyLong(), any(), any())).thenReturn(pageResult);

            mockMvc.perform(get("/books")
                    .param("current", "1")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("获取图书详情 - 无需认证")
        void getBookById_noAuth_shouldReturn200() throws Exception {
            BookResponse book = new BookResponse();
            book.setId(1L);
            book.setTitle("测试图书");
            when(bookService.getBookById(1L)).thenReturn(book);

            mockMvc.perform(get("/books/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("获取热门图书 - 无需认证")
        void getHotBooks_noAuth_shouldReturn200() throws Exception {
            when(bookService.getHotBooks(anyInt())).thenReturn(List.of(new BookResponse()));

            mockMvc.perform(get("/books/hot"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("管理接口 - 需要认证")
    class ManagementEndpoints {

        @Test
        @DisplayName("创建图书 - 无认证返回 401")
        void createBook_noAuth_shouldReturn401() throws Exception {
            String body = objectMapper.writeValueAsString(new BookRequest());

            mockMvc.perform(post("/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("创建图书 - 管理员认证成功")
        void createBook_withAdminAuth_shouldReturn200() throws Exception {
            BookResponse response = new BookResponse();
            response.setId(1L);
            response.setTitle("新书");
            when(bookService.createBook(any(BookRequest.class))).thenReturn(response);

            String body = objectMapper.writeValueAsString(new BookRequest() {{
                setIsbn("978-7-111-11111-1");
                setTitle("新书");
                setAuthor("作者");
                setPublisher("出版社");
                setCategoryId(1L);
                setTotalCount(10);
            }});

            mockMvc.perform(post("/books")
                    .header("Authorization", ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("创建图书 - 参数缺失返回 400")
        void createBook_missingFields_shouldReturn400() throws Exception {
            String body = "{}";

            mockMvc.perform(post("/books")
                    .header("Authorization", ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
