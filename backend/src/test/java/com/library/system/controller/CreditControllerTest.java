package com.library.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.CreditRecordResponse;
import com.library.system.service.CreditService;
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
 * 信用积分控制器单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(CreditController.class)
class CreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreditService creditService;

    private CreditRecordResponse testCreditRecord;

    @BeforeEach
    void setUp() {
        testCreditRecord = CreditRecordResponse.builder()
                .id(1L)
                .readerId(1L)
                .readerName("张三")
                .changeAmount(10)
                .currentScore(100)
                .reason("借书归还及时")
                .operatorId(1L)
                .operatorName("管理员")
                .createTime(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetCreditRecords_Success() throws Exception {
        List<CreditRecordResponse> records = Arrays.asList(testCreditRecord);
        when(creditService.getCreditRecords(anyLong())).thenReturn(records);

        mockMvc.perform(get("/credits/records")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].readerName").value("张三"))
                .andExpect(jsonPath("$.data[0].changeAmount").value(10))
                .andExpect(jsonPath("$.data[0].currentScore").value(100));

        verify(creditService).getCreditRecords(1L);
    }

    @Test
    void testGetCreditRecords_EmptyList() throws Exception {
        when(creditService.getCreditRecords(anyLong())).thenReturn(Arrays.asList());

        mockMvc.perform(get("/credits/records")
                        .param("readerId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(creditService).getCreditRecords(999L);
    }

    @Test
    void testGetCreditScore_Success() throws Exception {
        when(creditService.getCreditScore(anyLong())).thenReturn(100);

        mockMvc.perform(get("/credits/score")
                        .param("readerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(100));

        verify(creditService).getCreditScore(1L);
    }

    @Test
    void testGetCreditScore_ReaderNotFound() throws Exception {
        when(creditService.getCreditScore(anyLong()))
                .thenThrow(new RuntimeException("读者不存在"));

        mockMvc.perform(get("/credits/score")
                        .param("readerId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("读者不存在"));
    }

    @Test
    void testAddCreditScore_Success() throws Exception {
        doNothing().when(creditService).addCreditScore(anyLong(), anyInt(), anyString(), anyLong());

        mockMvc.perform(post("/credits/add")
                        .param("readerId", "1")
                        .param("changeAmount", "10")
                        .param("reason", "借书归还及时")
                        .param("operatorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("加分成功"));

        verify(creditService).addCreditScore(1L, 10, "借书归还及时", 1L);
    }

    @Test
    void testDeductCreditScore_Success() throws Exception {
        doNothing().when(creditService).deductCreditScore(anyLong(), anyInt(), anyString(), anyLong());

        mockMvc.perform(post("/credits/deduct")
                        .param("readerId", "1")
                        .param("changeAmount", "5")
                        .param("reason", "图书逾期")
                        .param("operatorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("扣分成功"));

        verify(creditService).deductCreditScore(1L, 5, "图书逾期", 1L);
    }

    @Test
    void testAddCreditScore_InvalidAmount() throws Exception {
        // Test with invalid amount (should be positive)
        mockMvc.perform(post("/credits/add")
                        .param("readerId", "1")
                        .param("changeAmount", "-10")  // Invalid negative amount
                        .param("reason", "test")
                        .param("operatorId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCreditRecords_WithPagination() throws Exception {
        List<CreditRecordResponse> records = Arrays.asList(testCreditRecord);
        when(creditService.getCreditRecords(anyLong())).thenReturn(records);

        mockMvc.perform(get("/credits/records")
                        .param("readerId", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(creditService).getCreditRecords(1L);
    }

    @Test
    void testGetCreditStatistics_Success() throws Exception {
        // Assuming there's a statistics endpoint
        when(creditService.getCreditStatistics()).thenReturn("statistics data");

        mockMvc.perform(get("/credits/statistics"))
                .andExpect(status().isOk());

        verify(creditService).getCreditStatistics();
    }
}
