package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookReservationResponse {

    private Long id;
    private Long userId;
    private Long bookId;
    private String bookTitle;
    private String status;
    private Integer queuePosition;
    private LocalDateTime createTime;
    private LocalDateTime notifyTime;
}
