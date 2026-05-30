package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.BorrowRequest;
import com.library.system.dto.BorrowResponse;
import com.library.system.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BorrowController 安全测试")
class BorrowControllerTest extends ControllerTestBase {

    @Mock
    private BorrowService borrowService;

    @InjectMocks
    private BorrowController borrowController;

    @BeforeEach
    void setUp() {
        initMockMvc(borrowController);
    }

    @Nested
    @DisplayName("借阅操作 - 需认证")
    class BorrowEndpoints {

        @Test
        @DisplayName("借书 - 无认证返回 401")
        void borrowBook_noAuth_shouldReturn401() throws Exception {
            mockMvc.perform(post("/borrows")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("借书 - 读者认证成功")
        void borrowBook_withAuth_shouldReturn200() throws Exception {
            when(borrowService.borrowBook(anyLong(), any(BorrowRequest.class)))
                    .thenReturn(new BorrowResponse());

            String body = objectMapper.writeValueAsString(new BorrowRequest() {{
                setBookId(1L);
                setBorrowDays(30);
            }});

            mockMvc.perform(post("/borrows")
                    .with(readerAuth())
                    .header("Authorization", READER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("还书 - 需要认证")
        void returnBook_withAuth_shouldReturn200() throws Exception {
            when(borrowService.returnBook(anyLong(), anyLong()))
                    .thenReturn(new BorrowResponse());

            mockMvc.perform(post("/borrows/100/return")
                    .with(readerAuth())
                    .header("Authorization", READER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("借书 - 缺少参数返回 400")
        void borrowBook_missingParams_shouldReturn400() throws Exception {
            String invalidBody = "{\"bookId\": null}";

            mockMvc.perform(post("/borrows")
                    .header("Authorization", READER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("查询接口 - 需认证")
    class QueryEndpoints {

        @Test
        @DisplayName("我的借阅 - 需认证")
        void getMyBorrows_noAuth_shouldReturn401() throws Exception {
            mockMvc.perform(get("/borrows/my")
                    .with(readerAuth()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("全部借阅 - 管理员权限")
        void getAllBorrows_adminAuth_shouldReturn200() throws Exception {
            mockMvc.perform(get("/borrows")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN))
                    .andExpect(status().isOk());
        }
    }
}
