package com.library.system.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerialClaimResponse {

    private Long id;

    private Long subscriptionId;

    private Long issueId;

    private String claimNumber;

    private Long vendorId;

    private String claimType;

    private String claimStatus;

    private LocalDate claimDate;

    private LocalDate responseDate;

    private String description;

    private String resolution;

    private Long operatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String subscriptionTitle;

    private String vendorName;
}
