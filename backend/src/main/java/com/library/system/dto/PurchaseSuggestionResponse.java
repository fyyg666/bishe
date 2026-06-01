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
public class PurchaseSuggestionResponse {

    private Long id;
    private Long userId;
    private String username;
    private String title;
    private String author;
    private String isbn;
    private String reason;
    private String status;
    private String statusDesc;
    private Long reviewerId;
    private String reviewerName;
    private String reviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
