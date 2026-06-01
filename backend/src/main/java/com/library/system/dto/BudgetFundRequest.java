package com.library.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetFundRequest {

    @NotBlank(message = "预算名称不能为空")
    private String name;

    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    @NotNull(message = "财政年度不能为空")
    private Integer fiscalYear;
}
