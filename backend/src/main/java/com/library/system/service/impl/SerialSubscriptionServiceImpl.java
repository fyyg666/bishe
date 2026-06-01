package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.entity.SerialIssue;
import com.library.system.entity.SerialSubscription;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.SerialIssueMapper;
import com.library.system.mapper.SerialSubscriptionMapper;
import com.library.system.service.SerialSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerialSubscriptionServiceImpl implements SerialSubscriptionService {

    private final SerialSubscriptionMapper subscriptionMapper;
    private final SerialIssueMapper issueMapper;

    private static final Map<String, Integer> FREQUENCY_DAYS = Map.of(
            "DAILY", 1,
            "WEEKLY", 7,
            "BIWEEKLY", 14,
            "MONTHLY", 30,
            "QUARTERLY", 90,
            "YEARLY", 365
    );

    @Override
    public PageResult<SerialSubscription> listSubscriptions(Long current, Long size, String status) {
        LambdaQueryWrapper<SerialSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SerialSubscription::getDeleted, 0);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(SerialSubscription::getStatus, status);
        }

        wrapper.orderByDesc(SerialSubscription::getCreateTime);

        Page<SerialSubscription> page = new Page<>(current, size);
        Page<SerialSubscription> resultPage = subscriptionMapper.selectPage(page, wrapper);

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    public SerialSubscription getSubscription(Long id) {
        SerialSubscription subscription = subscriptionMapper.selectById(id);
        if (subscription == null || subscription.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "期刊订阅不存在");
        }
        return subscription;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialSubscription createSubscription(SerialSubscription subscription) {
        subscription.setStatus(subscription.getStatus() != null ? subscription.getStatus() : "ACTIVE");
        subscription.setFrequency(subscription.getFrequency() != null ? subscription.getFrequency() : "MONTHLY");
        subscriptionMapper.insert(subscription);
        log.info("期刊订阅创建成功: {}", subscription.getTitle());
        return subscription;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialSubscription updateSubscription(Long id, SerialSubscription subscription) {
        SerialSubscription existing = subscriptionMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "期刊订阅不存在");
        }

        existing.setTitle(subscription.getTitle());
        existing.setIssn(subscription.getIssn());
        existing.setVendorId(subscription.getVendorId());
        existing.setFundId(subscription.getFundId());
        existing.setStartDate(subscription.getStartDate());
        existing.setEndDate(subscription.getEndDate());
        if (subscription.getFrequency() != null) {
            existing.setFrequency(subscription.getFrequency());
        }
        if (subscription.getStatus() != null) {
            existing.setStatus(subscription.getStatus());
        }

        subscriptionMapper.updateById(existing);
        log.info("期刊订阅更新成功: id={}", id);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubscription(Long id) {
        SerialSubscription existing = subscriptionMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "期刊订阅不存在");
        }

        subscriptionMapper.deleteById(id);
        log.info("期刊订阅删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateExpectedIssues(Long subscriptionId) {
        SerialSubscription subscription = getSubscription(subscriptionId);

        if (subscription.getStartDate() == null || subscription.getEndDate() == null) {
            throw new ResourceNotFoundException(ErrorCode.PARAMETER_ERROR, "订阅日期不完整，无法生成预期到刊");
        }

        Integer daysPerIssue = FREQUENCY_DAYS.get(subscription.getFrequency());
        if (daysPerIssue == null) {
            throw new ResourceNotFoundException(ErrorCode.PARAMETER_ERROR, "未知的出版频率: " + subscription.getFrequency());
        }

        LambdaQueryWrapper<SerialIssue> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SerialIssue::getSubscriptionId, subscriptionId)
                .eq(SerialIssue::getStatus, "EXPECTED");
        issueMapper.delete(deleteWrapper);

        LocalDate startDate = subscription.getStartDate();
        LocalDate endDate = subscription.getEndDate();
        LocalDate current = startDate;
        int issueNumber = 1;

        List<SerialIssue> issues = new ArrayList<>();
        while (!current.isAfter(endDate)) {
            SerialIssue issue = new SerialIssue();
            issue.setSubscriptionId(subscriptionId);
            issue.setVolume(String.valueOf(startDate.getYear()));
            issue.setIssue(String.valueOf(issueNumber));
            issue.setExpectedDate(current);
            issue.setStatus("EXPECTED");
            issues.add(issue);

            issueNumber++;
            current = current.plusDays(daysPerIssue);
        }

        for (SerialIssue issue : issues) {
            issueMapper.insert(issue);
        }

        log.info("为订阅ID={}生成了{}条预期到刊记录", subscriptionId, issues.size());
    }
}
