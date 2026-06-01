package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRuleResponse {

    private Long id;
    private String readerType;
    private String bookType;
    private Integer maxBorrow;
    private Integer maxDays;
    private Integer maxRenew;
    private Integer renewDays;
    private BigDecimal finePerDay;
}
