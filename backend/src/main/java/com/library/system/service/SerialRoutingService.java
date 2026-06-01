package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.SerialRoutingRequest;
import com.library.system.dto.SerialRoutingResponse;
import com.library.system.dto.SerialRoutingTemplateRequest;
import com.library.system.entity.SerialRoutingTemplate;

import java.util.List;

public interface SerialRoutingService {

    PageResult<SerialRoutingResponse> listRoutings(Long current, Long size, String routingStatus, String destination);

    SerialRoutingResponse getRouting(Long id);

    SerialRoutingResponse createRouting(SerialRoutingRequest request);

    void batchCreateRoutings(Long issueId, Long subscriptionId);

    SerialRoutingResponse sendRouting(Long id);

    SerialRoutingResponse deliverRouting(Long id, String receivedBy);

    void deleteRouting(Long id);

    List<SerialRoutingTemplate> listTemplates(Long subscriptionId);

    SerialRoutingTemplate createTemplate(SerialRoutingTemplateRequest request);

    void deleteTemplate(Long id);

    void applyTemplate(Long issueId, Long subscriptionId);
}
