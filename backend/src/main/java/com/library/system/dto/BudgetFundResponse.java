package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetFundResponse {

    private Long id;
    private String name;
    private BigDecimal totalAmount;
    private BigDecimal usedAmount;
    private BigDecimal remaining;
    private Integer fiscalYear;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
