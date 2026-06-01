package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.service.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CreditController 安全测试")
class CreditControllerTest extends ControllerTestBase {

    @Mock
    private CreditService creditService;

    @InjectMocks
    private CreditController creditController;

    @BeforeEach
    void setUp() {
        initMockMvc(creditController);
    }

    @Nested
    @DisplayName("积分查询")
    class QueryEndpoints {
        @Test
        void getCredit_shouldReturn200() throws Exception {
            when(creditService.getUserCredit(any())).thenReturn(100);
            mockMvc.perform(get("/credits")
                    .with(readerAuth())
                    .header("Authorization", READER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test
        void getCreditLogs_shouldReturn200() throws Exception {
            mockMvc.perform(get("/credits/logs")
                    .with(readerAuth())
                    .header("Authorization", READER_TOKEN))
                    .andExpect(status().isOk());
        }
    }
}
