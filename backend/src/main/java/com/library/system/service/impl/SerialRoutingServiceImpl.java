package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.dto.SerialRoutingRequest;
import com.library.system.dto.SerialRoutingResponse;
import com.library.system.dto.SerialRoutingTemplateRequest;
import com.library.system.entity.SerialIssue;
import com.library.system.entity.SerialRouting;
import com.library.system.entity.SerialRoutingTemplate;
import com.library.system.entity.SerialSubscription;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.SerialIssueMapper;
import com.library.system.mapper.SerialRoutingMapper;
import com.library.system.mapper.SerialRoutingTemplateMapper;
import com.library.system.mapper.SerialSubscriptionMapper;
import com.library.system.service.SerialRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerialRoutingServiceImpl implements SerialRoutingService {

    private final SerialRoutingMapper routingMapper;
    private final SerialRoutingTemplateMapper templateMapper;
    private final SerialSubscriptionMapper subscriptionMapper;
    private final SerialIssueMapper issueMapper;

    @Override
    public PageResult<SerialRoutingResponse> listRoutings(Long current, Long size, String routingStatus, String destination) {
        LambdaQueryWrapper<SerialRouting> wrapper = new LambdaQueryWrapper<>();
        if (routingStatus != null && !routingStatus.isEmpty()) {
            wrapper.eq(SerialRouting::getRoutingStatus, routingStatus);
        }
        if (destination != null && !destination.isEmpty()) {
            wrapper.like(SerialRouting::getDestination, destination);
        }
        wrapper.orderByAsc(SerialRouting::getRoutingOrder);

        Page<SerialRouting> page = new Page<>(current, size);
        Page<SerialRouting> resultPage = routingMapper.selectPage(page, wrapper);

        List<SerialRoutingResponse> responses = new ArrayList<>();
        for (SerialRouting routing : resultPage.getRecords()) {
            responses.add(toResponse(routing));
        }

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), responses);
    }

    @Override
    public SerialRoutingResponse getRouting(Long id) {
        SerialRouting routing = routingMapper.selectById(id);
        if (routing == null) {
            throw new ResourceNotFoundException("期刊路由分发记录不存在");
        }
        return toResponse(routing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialRoutingResponse createRouting(SerialRoutingRequest request) {
        SerialRouting routing = new SerialRouting();
        routing.setSubscriptionId(request.getSubscriptionId());
        routing.setIssueId(request.getIssueId());
        routing.setDestination(request.getDestination());
        routing.setCopies(request.getCopies() != null ? request.getCopies() : 1);
        routing.setRoutingOrder(request.getRoutingOrder() != null ? request.getRoutingOrder() : 1);
        routing.setRoutingStatus("PENDING");
        routing.setNotes(request.getNotes());
        routingMapper.insert(routing);
        log.info("创建期刊路由分发: id={}", routing.getId());
        return toResponse(routing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateRoutings(Long issueId, Long subscriptionId) {
        LambdaQueryWrapper<SerialRoutingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SerialRoutingTemplate::getSubscriptionId, subscriptionId);
        wrapper.orderByAsc(SerialRoutingTemplate::getRoutingOrder);
        List<SerialRoutingTemplate> templates = templateMapper.selectList(wrapper);

        for (SerialRoutingTemplate template : templates) {
            SerialRouting routing = new SerialRouting();
            routing.setSubscriptionId(subscriptionId);
            routing.setIssueId(issueId);
            routing.setDestination(template.getDestination());
            routing.setCopies(template.getCopies());
            routing.setRoutingOrder(template.getRoutingOrder());
            routing.setRoutingStatus("PENDING");
            routingMapper.insert(routing);
        }
        log.info("批量创建期刊路由分发: issueId={}, subscriptionId={}, count={}", issueId, subscriptionId, templates.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialRoutingResponse sendRouting(Long id) {
        SerialRouting routing = routingMapper.selectById(id);
        if (routing == null) {
            throw new ResourceNotFoundException("期刊路由分发记录不存在");
        }
        routing.setRoutingStatus("IN_TRANSIT");
        routing.setSentDate(LocalDate.now());
        routingMapper.updateById(routing);
        log.info("期刊路由发出: id={}", id);
        return toResponse(routing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialRoutingResponse deliverRouting(Long id, String receivedBy) {
        SerialRouting routing = routingMapper.selectById(id);
        if (routing == null) {
            throw new ResourceNotFoundException("期刊路由分发记录不存在");
        }
        routing.setRoutingStatus("DELIVERED");
        routing.setReceivedBy(receivedBy);
        routing.setReceivedDate(LocalDate.now());
        routingMapper.updateById(routing);
        log.info("期刊路由签收: id={}, receivedBy={}", id, receivedBy);
        return toResponse(routing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRouting(Long id) {
        SerialRouting routing = routingMapper.selectById(id);
        if (routing == null) {
            throw new ResourceNotFoundException("期刊路由分发记录不存在");
        }
        routingMapper.deleteById(id);
        log.info("删除期刊路由分发: id={}", id);
    }

    @Override
    public List<SerialRoutingTemplate> listTemplates(Long subscriptionId) {
        LambdaQueryWrapper<SerialRoutingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SerialRoutingTemplate::getSubscriptionId, subscriptionId);
        wrapper.orderByAsc(SerialRoutingTemplate::getRoutingOrder);
        return templateMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialRoutingTemplate createTemplate(SerialRoutingTemplateRequest request) {
        SerialRoutingTemplate template = new SerialRoutingTemplate();
        template.setSubscriptionId(request.getSubscriptionId());
        template.setDestination(request.getDestination());
        template.setCopies(request.getCopies() != null ? request.getCopies() : 1);
        template.setRoutingOrder(request.getRoutingOrder() != null ? request.getRoutingOrder() : 1);
        templateMapper.insert(template);
        log.info("创建期刊路由分发模板: id={}", template.getId());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        SerialRoutingTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ResourceNotFoundException("期刊路由分发模板不存在");
        }
        templateMapper.deleteById(id);
        log.info("删除期刊路由分发模板: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyTemplate(Long issueId, Long subscriptionId) {
        batchCreateRoutings(issueId, subscriptionId);
    }

    private SerialRoutingResponse toResponse(SerialRouting routing) {
        SerialRoutingResponse response = new SerialRoutingResponse();
        response.setId(routing.getId());
        response.setSubscriptionId(routing.getSubscriptionId());
        response.setIssueId(routing.getIssueId());
        response.setDestination(routing.getDestination());
        response.setCopies(routing.getCopies());
        response.setRoutingOrder(routing.getRoutingOrder());
        response.setRoutingStatus(routing.getRoutingStatus());
        response.setSentDate(routing.getSentDate());
        response.setReceivedBy(routing.getReceivedBy());
        response.setReceivedDate(routing.getReceivedDate());
        response.setNotes(routing.getNotes());
        response.setCreateTime(routing.getCreateTime());
        response.setUpdateTime(routing.getUpdateTime());

        if (routing.getSubscriptionId() != null) {
            SerialSubscription subscription = subscriptionMapper.selectById(routing.getSubscriptionId());
            if (subscription != null) {
                response.setSubscriptionTitle(subscription.getTitle());
            }
        }

        if (routing.getIssueId() != null) {
            SerialIssue issue = issueMapper.selectById(routing.getIssueId());
            if (issue != null) {
                response.setIssueVolume(issue.getVolume());
                response.setIssueIssue(issue.getIssue());
            }
        }

        return response;
    }
}
