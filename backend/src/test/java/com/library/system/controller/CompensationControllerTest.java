package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.CompensationRequest;
import com.library.system.service.CompensationService;
import com.library.system.dto.CompensationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CompensationController 测试")
class CompensationControllerTest extends ControllerTestBase {

    @Mock
    private CompensationService compensationService;

    @InjectMocks
    private CompensationController compensationController;

    @BeforeEach
    void setUp() {
        initMockMvc(compensationController);
    }

    @Nested
    @DisplayName("赔偿查询")
    class QueryEndpoints {
        @Test
        void listCompensations_shouldReturn200() throws Exception {
            mockMvc.perform(get("/compensations")
                    .header("Authorization", ADMIN_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("赔偿操作")
    class OperationEndpoints {
        @Test
        void createCompensation_shouldReturn200() throws Exception {
            when(compensationService.createCompensation(any(CompensationRequest.class), anyLong()))
                    .thenReturn(new CompensationResponse());

            String body = objectMapper.writeValueAsString(new CompensationRequest() {{
                setUserId(1L);
                setBookId(2L);
                setBookTitle("测试图书");
                setCompType("DAMAGE");
                setPaymentMethod("CASH");
                setAmount(new BigDecimal("50.00"));
            }});

            mockMvc.perform(post("/compensations")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        void processCashPayment_shouldReturn200() throws Exception {
            when(compensationService.processCashPayment(any(), any(), any()))
                    .thenReturn(new CompensationResponse());

            mockMvc.perform(post("/compensations/1/pay/cash")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN))
                    .andExpect(status().isOk());
        }
    }
}
