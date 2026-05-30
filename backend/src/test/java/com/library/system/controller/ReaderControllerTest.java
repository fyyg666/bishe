package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.PageResult;
import com.library.system.dto.ReaderResponse;
import com.library.system.service.ReaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ReaderController 测试")
class ReaderControllerTest extends ControllerTestBase {

    @Mock
    private ReaderService readerService;

    @InjectMocks
    private ReaderController readerController;

    @BeforeEach
    void setUp() {
        initMockMvc(readerController);
    }

    @Nested
    @DisplayName("查询接口")
    class QueryEndpoints {

        @Test
        @DisplayName("获取读者列表 - 应返回200")
        void listReaders_shouldReturn200() throws Exception {
            PageResult<ReaderResponse> pageResult = new PageResult<>();
            pageResult.setTotal(0L);
            pageResult.setRecords(List.of());
            when(readerService.listReaders(anyLong(), anyLong(), any(), any())).thenReturn(pageResult);

            mockMvc.perform(get("/readers")
                    .param("current", "1")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("获取读者详情 - 应返回200")
        void getReaderById_shouldReturn200() throws Exception {
            ReaderResponse reader = new ReaderResponse();
            reader.setId(1L);
            reader.setUsername("reader1");
            when(readerService.getReaderById(1L)).thenReturn(reader);

            mockMvc.perform(get("/readers/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
