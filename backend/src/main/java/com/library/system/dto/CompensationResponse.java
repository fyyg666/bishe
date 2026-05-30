package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 赔偿响应DTO
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationResponse {

    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private Long borrowId;
    private Long bookId;
    private String bookTitle;
    private String isbn;
    private String compType;
    private String compTypeDesc;
    private BigDecimal amount;
    private String status;
    private String statusDesc;
    private String paymentMethod;
    private String paymentMethodDesc;
    private Integer creditDeducted;
    private BigDecimal volunteerHours;
    private String remark;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
