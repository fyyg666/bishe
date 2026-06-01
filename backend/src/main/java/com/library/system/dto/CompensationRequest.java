package com.library.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 赔偿请求DTO
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private Long borrowId;

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @NotBlank(message = "书名不能为空")
    private String bookTitle;

    private String isbn;

    @NotBlank(message = "赔偿类型不能为空")
    @Pattern(regexp = "LOST|DAMAGE", message = "赔偿类型必须为LOST或DAMAGE")
    private String compType;

    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "CASH|CREDIT|VOLUNTEER", message = "支付方式必须为CASH、CREDIT或VOLUNTEER")
    private String paymentMethod;

    private Integer creditAmount;

    private BigDecimal volunteerHours;

    private String remark;
}
