package com.library.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 借阅请求DTO 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequest {

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    /**
     * 借阅天数（默认30天）
     */
    @Min(value = 1, message = "借阅天数不能小于1天")
    @Max(value = 60, message = "借阅天数不能超过60天")
    private Integer borrowDays;
}
