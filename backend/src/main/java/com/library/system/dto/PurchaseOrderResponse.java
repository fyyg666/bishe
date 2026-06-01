package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {

    private Long id;
    private String orderNo;
    private Long vendorId;
    private String vendorName;
    private String status;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime approveTime;
    private LocalDateTime updateTime;
    private List<OrderItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private Long id;
        private String bookTitle;
        private String isbn;
        private Integer quantity;
        private BigDecimal unitPrice;
        private Integer receivedQuantity;
    }
}
