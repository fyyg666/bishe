package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.PageResult;
import com.library.system.dto.SerialClaimRequest;
import com.library.system.dto.SerialClaimResponse;
import com.library.system.entity.SerialClaim;
import com.library.system.entity.SerialIssue;
import com.library.system.entity.SerialSubscription;
import com.library.system.entity.Vendor;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.SerialClaimMapper;
import com.library.system.mapper.SerialIssueMapper;
import com.library.system.mapper.SerialSubscriptionMapper;
import com.library.system.mapper.VendorMapper;
import com.library.system.service.SerialClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerialClaimServiceImpl implements SerialClaimService {

    private final SerialClaimMapper claimMapper;
    private final SerialSubscriptionMapper subscriptionMapper;
    private final SerialIssueMapper issueMapper;
    private final VendorMapper vendorMapper;

    @Override
    public PageResult<SerialClaimResponse> listClaims(Long current, Long size, String claimStatus, String claimType) {
        LambdaQueryWrapper<SerialClaim> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(claimStatus)) {
            wrapper.eq(SerialClaim::getClaimStatus, claimStatus);
        }
        if (StringUtils.hasText(claimType)) {
            wrapper.eq(SerialClaim::getClaimType, claimType);
        }
        wrapper.orderByDesc(SerialClaim::getCreateTime);

        Page<SerialClaim> page = new Page<>(current, size);
        Page<SerialClaim> resultPage = claimMapper.selectPage(page, wrapper);

        List<SerialClaimResponse> records = resultPage.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), records);
    }

    @Override
    public SerialClaimResponse getClaim(Long id) {
        SerialClaim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw new ResourceNotFoundException("催缺记录不存在");
        }
        return toResponse(claim);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialClaimResponse createClaim(SerialClaimRequest request, Long operatorId) {
        SerialSubscription subscription = subscriptionMapper.selectById(request.getSubscriptionId());
        if (subscription == null) {
            throw new ResourceNotFoundException("期刊订阅不存在");
        }

        SerialClaim claim = new SerialClaim();
        claim.setSubscriptionId(request.getSubscriptionId());
        claim.setIssueId(request.getIssueId());
        claim.setVendorId(request.getVendorId());
        claim.setClaimType(request.getClaimType());
        claim.setClaimDate(request.getClaimDate());
        claim.setDescription(request.getDescription());
        claim.setClaimStatus("PENDING");
        claim.setOperatorId(operatorId);
        claim.setClaimNumber(generateClaimNumber());

        claimMapper.insert(claim);
        log.info("创建催缺记录: claimNumber={}, subscriptionId={}", claim.getClaimNumber(), claim.getSubscriptionId());
        return toResponse(claim);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SerialClaimResponse updateClaim(Long id, SerialClaimRequest request) {
        SerialClaim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw new ResourceNotFoundException("催缺记录不存在");
        }

        claim.setSubscriptionId(request.getSubscriptionId());
        claim.setIssueId(request.getIssueId());
        claim.setVendorId(request.getVendorId());
        claim.setClaimType(request.getClaimType());
        claim.setClaimDate(request.getClaimDate());
        claim.setDescription(request.getDescription());

        claimMapper.updateById(claim);
        log.info("更新催缺记录: id={}", id);
        return toResponse(claim);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveClaim(Long id, String resolution) {
        SerialClaim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw new ResourceNotFoundException("催缺记录不存在");
        }

        claim.setClaimStatus("RESOLVED");
        claim.setResponseDate(LocalDate.now());
        claim.setResolution(resolution);
        claimMapper.updateById(claim);
        log.info("催缺记录已处理: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeClaim(Long id) {
        SerialClaim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw new ResourceNotFoundException("催缺记录不存在");
        }

        claim.setClaimStatus("CLOSED");
        claimMapper.updateById(claim);
        log.info("催缺记录已关闭: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int checkAndAutoClaim() {
        LocalDate threshold = LocalDate.now().minusDays(30);

        LambdaQueryWrapper<SerialIssue> issueWrapper = new LambdaQueryWrapper<>();
        issueWrapper.eq(SerialIssue::getStatus, "MISSING")
                .lt(SerialIssue::getExpectedDate, threshold);
        List<SerialIssue> missingIssues = issueMapper.selectList(issueWrapper);

        int count = 0;
        for (SerialIssue issue : missingIssues) {
            LambdaQueryWrapper<SerialClaim> claimWrapper = new LambdaQueryWrapper<>();
            claimWrapper.eq(SerialClaim::getIssueId, issue.getId())
                    .ne(SerialClaim::getClaimStatus, "CLOSED");
            Long existingCount = claimMapper.selectCount(claimWrapper);

            if (existingCount == 0) {
                SerialClaim claim = new SerialClaim();
                claim.setSubscriptionId(issue.getSubscriptionId());
                claim.setIssueId(issue.getId());
                claim.setClaimType("MISSING");
                claim.setClaimStatus("PENDING");
                claim.setClaimDate(LocalDate.now());
                claim.setOperatorId(0L);
                claim.setClaimNumber(generateClaimNumber());

                SerialSubscription subscription = subscriptionMapper.selectById(issue.getSubscriptionId());
                if (subscription != null && subscription.getVendorId() != null) {
                    claim.setVendorId(subscription.getVendorId());
                }

                claimMapper.insert(claim);
                count++;
            }
        }

        if (count > 0) {
            log.info("自动生成催缺记录: {}条缺刊超过30天", count);
        }
        return count;
    }

    private String generateClaimNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CL-" + datePart + "-";

        LambdaQueryWrapper<SerialClaim> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SerialClaim::getClaimNumber, prefix)
                .orderByDesc(SerialClaim::getClaimNumber)
                .last("LIMIT 1");
        SerialClaim lastClaim = claimMapper.selectOne(wrapper);

        int seq = 1;
        if (lastClaim != null && lastClaim.getClaimNumber() != null) {
            String lastNumber = lastClaim.getClaimNumber().substring(prefix.length());
            try {
                seq = Integer.parseInt(lastNumber) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        return prefix + String.format("%03d", seq);
    }

    private SerialClaimResponse toResponse(SerialClaim claim) {
        SerialClaimResponse response = SerialClaimResponse.builder()
                .id(claim.getId())
                .subscriptionId(claim.getSubscriptionId())
                .issueId(claim.getIssueId())
                .claimNumber(claim.getClaimNumber())
                .vendorId(claim.getVendorId())
                .claimType(claim.getClaimType())
                .claimStatus(claim.getClaimStatus())
                .claimDate(claim.getClaimDate())
                .responseDate(claim.getResponseDate())
                .description(claim.getDescription())
                .resolution(claim.getResolution())
                .operatorId(claim.getOperatorId())
                .createTime(claim.getCreateTime())
                .updateTime(claim.getUpdateTime())
                .build();

        if (claim.getSubscriptionId() != null) {
            SerialSubscription subscription = subscriptionMapper.selectById(claim.getSubscriptionId());
            if (subscription != null) {
                response.setSubscriptionTitle(subscription.getTitle());
            }
        }

        if (claim.getVendorId() != null) {
            Vendor vendor = vendorMapper.selectById(claim.getVendorId());
            if (vendor != null) {
                response.setVendorName(vendor.getName());
            }
        }

        return response;
    }
}
