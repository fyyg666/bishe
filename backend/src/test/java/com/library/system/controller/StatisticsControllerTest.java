package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.BookResponse;
import com.library.system.service.StatisticsService;
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

@DisplayName("StatisticsController 测试")
class StatisticsControllerTest extends ControllerTestBase {

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private StatisticsController statisticsController;

    @BeforeEach
    void setUp() {
        initMockMvc(statisticsController);
    }

    @Nested
    @DisplayName("统计查询接口")
    class StatsEndpoints {

        @Test
        @DisplayName("获取热门图书 - 公开接口应返回200")
        void getHotBooks_shouldReturn200() throws Exception {
            when(statisticsService.getHotBooks(anyInt())).thenReturn(List.of(new BookResponse()));

            mockMvc.perform(get("/statistics/hot-books"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
