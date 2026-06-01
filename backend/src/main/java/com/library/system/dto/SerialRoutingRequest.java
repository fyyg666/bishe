package com.library.system.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerialRoutingRequest {

    private Long subscriptionId;

    private Long issueId;

    private String destination;

    private Integer copies;

    private Integer routingOrder;

    private String notes;
}
