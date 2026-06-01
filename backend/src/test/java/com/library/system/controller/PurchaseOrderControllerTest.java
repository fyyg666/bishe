package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.PurchaseOrderRequest;
import com.library.system.dto.PurchaseOrderResponse;
import com.library.system.service.PurchaseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PurchaseOrderController 测试")
class PurchaseOrderControllerTest extends ControllerTestBase {

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseOrderController purchaseOrderController;

    @BeforeEach
    void setUp() {
        initMockMvc(purchaseOrderController);
    }

    @Nested
    @DisplayName("创建订单")
    class CreateOrder {
        @Test
        @DisplayName("创建订单 - items为空列表应返回400")
        void createOrder_emptyItems_shouldReturn400() throws Exception {
            PurchaseOrderRequest request = new PurchaseOrderRequest();
            request.setVendorId(1L);
            request.setItems(new ArrayList<>());
            String body = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/purchase-orders")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("创建订单 - 正常应返回200")
        void createOrder_valid_shouldReturn200() throws Exception {
            PurchaseOrderRequest.ItemRequest item = new PurchaseOrderRequest.ItemRequest();
            item.setBookTitle("测试图书");
            item.setQuantity(10);
            item.setUnitPrice(BigDecimal.valueOf(29.90));
            PurchaseOrderRequest request = new PurchaseOrderRequest();
            request.setVendorId(1L);
            request.setItems(List.of(item));
            when(purchaseOrderService.createOrder(any(PurchaseOrderRequest.class)))
                    .thenReturn(new PurchaseOrderResponse());
            String body = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/purchase-orders")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("订单审批")
    class ApproveOrder {
        @Test
        @DisplayName("审批订单 - LIBRARIAN审批应返回403")
        void approveOrder_asLibrarian_shouldReturn403() throws Exception {
            mockMvc.perform(post("/purchase-orders/1/approve")
                    .header("Authorization", READER_TOKEN))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("审批订单 - ADMIN应返回200")
        void approveOrder_asAdmin_shouldReturn200() throws Exception {
            when(purchaseOrderService.approveOrder(1L)).thenReturn(new PurchaseOrderResponse());
            mockMvc.perform(post("/purchase-orders/1/approve")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("收货入库")
    class ReceiveItems {
        @Test
        @DisplayName("收货入库 - 正常应返回200")
        void receiveItems_valid_shouldReturn200() throws Exception {
            when(purchaseOrderService.receiveItems(eq(1L), eq(1L), anyInt()))
                    .thenReturn(new PurchaseOrderResponse());
            mockMvc.perform(post("/purchase-orders/1/items/1/receive")
                    .param("receivedQty", "5")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN))
                    .andExpect(status().isOk());
        }
    }
}
