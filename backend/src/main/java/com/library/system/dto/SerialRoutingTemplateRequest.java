package com.library.system.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerialRoutingTemplateRequest {

    private Long subscriptionId;

    private String destination;

    private Integer copies;

    private Integer routingOrder;
}
