package com.library.system.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerialClaimRequest {

    private Long subscriptionId;

    private Long issueId;

    private Long vendorId;

    private String claimType;

    private LocalDate claimDate;

    private String description;
}
