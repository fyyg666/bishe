package com.library.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull(message = "供应商ID不能为空")
    private Long vendorId;

    private String remark;

    @Valid
    @NotNull(message = "采购明细不能为空")
    private List<ItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        @NotBlank(message = "书名不能为空")
        private String bookTitle;

        private String isbn;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        private Integer quantity;

        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;
    }
}
