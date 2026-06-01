package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.BorrowRuleRequest;
import com.library.system.dto.BorrowRuleResponse;
import com.library.system.entity.BorrowRule;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BorrowRuleMapper;
import com.library.system.service.BorrowRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowRuleServiceImpl implements BorrowRuleService {

    private final BorrowRuleMapper borrowRuleMapper;

    @Override
    public List<BorrowRuleResponse> listRules() {
        return borrowRuleMapper.selectList(new LambdaQueryWrapper<BorrowRule>()
                        .eq(BorrowRule::getDeleted, 0)
                        .orderByAsc(BorrowRule::getReaderType)
                        .orderByAsc(BorrowRule::getBookType))
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "borrowRuleCache", key = "#readerType + '_' + #bookType")
    public BorrowRuleResponse getRuleByType(String readerType, String bookType) {
        BorrowRule rule = getRuleEntity(readerType, bookType);
        return convertToResponse(rule);
    }

    @Override
    public BorrowRule getRuleEntity(String readerType, String bookType) {
        LambdaQueryWrapper<BorrowRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRule::getReaderType, readerType)
               .eq(BorrowRule::getBookType, bookType)
               .eq(BorrowRule::getDeleted, 0);
        BorrowRule rule = borrowRuleMapper.selectOne(wrapper);
        if (rule == null) {
            rule = borrowRuleMapper.selectOne(new LambdaQueryWrapper<BorrowRule>()
                    .eq(BorrowRule::getReaderType, readerType)
                    .eq(BorrowRule::getBookType, "NORMAL")
                    .eq(BorrowRule::getDeleted, 0));
        }
        if (rule == null) {
            rule = BorrowRule.builder()
                    .maxBorrow(5).maxDays(30).maxRenew(1).renewDays(15)
                    .finePerDay(java.math.BigDecimal.valueOf(0.10))
                    .build();
        }
        return rule;
    }

    @Override
    @CacheEvict(value = "borrowRuleCache", allEntries = true)
    public BorrowRuleResponse createRule(BorrowRuleRequest request) {
        BorrowRule rule = BorrowRule.builder()
                .readerType(request.getReaderType())
                .bookType(request.getBookType())
                .maxBorrow(request.getMaxBorrow())
                .maxDays(request.getMaxDays())
                .maxRenew(request.getMaxRenew())
                .renewDays(request.getRenewDays())
                .finePerDay(request.getFinePerDay())
                .build();
        borrowRuleMapper.insert(rule);
        return convertToResponse(rule);
    }

    @Override
    @CacheEvict(value = "borrowRuleCache", allEntries = true)
    public BorrowRuleResponse updateRule(Long id, BorrowRuleRequest request) {
        BorrowRule rule = borrowRuleMapper.selectById(id);
        if (rule == null || rule.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.PARAMETER_ERROR, "规则不存在");
        }
        rule.setReaderType(request.getReaderType());
        rule.setBookType(request.getBookType());
        rule.setMaxBorrow(request.getMaxBorrow());
        rule.setMaxDays(request.getMaxDays());
        rule.setMaxRenew(request.getMaxRenew());
        rule.setRenewDays(request.getRenewDays());
        rule.setFinePerDay(request.getFinePerDay());
        borrowRuleMapper.updateById(rule);
        return convertToResponse(rule);
    }

    @Override
    @CacheEvict(value = "borrowRuleCache", allEntries = true)
    public void deleteRule(Long id) {
        BorrowRule rule = borrowRuleMapper.selectById(id);
        if (rule != null) {
            borrowRuleMapper.deleteById(id);
        }
    }

    private BorrowRuleResponse convertToResponse(BorrowRule rule) {
        return BorrowRuleResponse.builder()
                .id(rule.getId())
                .readerType(rule.getReaderType())
                .bookType(rule.getBookType())
                .maxBorrow(rule.getMaxBorrow())
                .maxDays(rule.getMaxDays())
                .maxRenew(rule.getMaxRenew())
                .renewDays(rule.getRenewDays())
                .finePerDay(rule.getFinePerDay())
                .build();
    }
}
