package com.library.system.event;

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
public class BorrowEvent {

    private String type;
    private Long userId;
    private Long borrowId;
    private String bookTitle;
    private LocalDateTime dueDate;
    private Integer overdueDays;
    private BigDecimal fineAmount;
    private LocalDateTime returnDate;
}
