package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.entity.SerialIssue;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.SerialIssueMapper;
import com.library.system.service.SerialIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerialIssueServiceImpl implements SerialIssueService {

    private final SerialIssueMapper issueMapper;

    @Override
    public PageResult<SerialIssue> listIssues(Long subscriptionId, Long current, Long size) {
        LambdaQueryWrapper<SerialIssue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SerialIssue::getSubscriptionId, subscriptionId);
        wrapper.orderByAsc(SerialIssue::getExpectedDate);

        Page<SerialIssue> page = new Page<>(current, size);
        Page<SerialIssue> resultPage = issueMapper.selectPage(page, wrapper);

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialIssue receiveIssue(Long issueId) {
        SerialIssue issue = issueMapper.selectById(issueId);
        if (issue == null) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "期刊到刊记录不存在");
        }

        issue.setStatus("RECEIVED");
        issue.setReceivedDate(LocalDate.now());
        issueMapper.updateById(issue);
        log.info("期刊到刊登记成功: issueId={}", issueId);
        return issue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialIssue markMissing(Long issueId) {
        SerialIssue issue = issueMapper.selectById(issueId);
        if (issue == null) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "期刊到刊记录不存在");
        }

        issue.setStatus("MISSING");
        issueMapper.updateById(issue);
        log.info("期刊标记缺刊: issueId={}", issueId);
        return issue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int checkOverdueIssues() {
        LocalDate threshold = LocalDate.now().minusDays(30);

        LambdaUpdateWrapper<SerialIssue> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SerialIssue::getStatus, "EXPECTED")
                .lt(SerialIssue::getExpectedDate, threshold)
                .set(SerialIssue::getStatus, "MISSING");

        int count = issueMapper.update(null, updateWrapper);
        if (count > 0) {
            log.info("自动标记缺刊: {}条预期到刊已超过30天未到", count);
        }
        return count;
    }
}
