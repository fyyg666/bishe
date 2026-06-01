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
public class BorrowRuleRequest {

    @NotBlank(message = "读者类型不能为空")
    private String readerType;

    @NotBlank(message = "图书类型不能为空")
    private String bookType;

    @NotNull(message = "最大借阅数量不能为空")
    private Integer maxBorrow;

    @NotNull(message = "最大借阅天数不能为空")
    private Integer maxDays;

    @NotNull(message = "最大续借次数不能为空")
    private Integer maxRenew;

    @NotNull(message = "续借天数不能为空")
    private Integer renewDays;

    @NotNull(message = "每日罚款金额不能为空")
    private BigDecimal finePerDay;
}
