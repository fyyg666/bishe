package com.library.system.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerialRoutingResponse {

    private Long id;

    private Long subscriptionId;

    private Long issueId;

    private String destination;

    private Integer copies;

    private Integer routingOrder;

    private String routingStatus;

    private LocalDate sentDate;

    private String receivedBy;

    private LocalDate receivedDate;

    private String notes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String subscriptionTitle;

    private String issueVolume;

    private String issueIssue;
}
