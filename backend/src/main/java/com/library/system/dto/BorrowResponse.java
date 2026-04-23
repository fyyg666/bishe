package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowResponse {

    /**
     * 借阅记录ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String bookTitle;

    /**
     * 图书ISBN
     */
    private String bookIsbn;

    /**
     * 借阅日期
     */
    private LocalDateTime borrowDate;

    /**
     * 应还日期
     */
    private LocalDateTime dueDate;

    /**
     * 实际归还日期
     */
    private LocalDateTime returnDate;

    /**
     * 借阅状态
     */
    private String status;

    /**
     * 续借次数
     */
    private Integer renewCount;

    /**
     * 逾期天数
     */
    private Integer overdueDays;

    /**
     * 逾期罚款金额
     */
    private BigDecimal fineAmount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
