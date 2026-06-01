package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.BudgetFundRequest;
import com.library.system.dto.BudgetFundResponse;
import com.library.system.entity.BudgetFund;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BudgetFundMapper;
import com.library.system.service.BudgetFundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetFundServiceImpl implements BudgetFundService {

    private final BudgetFundMapper budgetFundMapper;

    @Override
    public List<BudgetFundResponse> listFunds(Integer fiscalYear) {
        LambdaQueryWrapper<BudgetFund> wrapper = new LambdaQueryWrapper<>();
        if (fiscalYear != null) {
            wrapper.eq(BudgetFund::getFiscalYear, fiscalYear);
        }
        wrapper.orderByDesc(BudgetFund::getCreateTime);
        return budgetFundMapper.selectList(wrapper).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BudgetFundResponse getFund(Long id) {
        BudgetFund fund = budgetFundMapper.selectById(id);
        if (fund == null || fund.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BUDGET_FUND_NOT_FOUND, "预算资金不存在");
        }
        return convertToResponse(fund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BudgetFundResponse createFund(BudgetFundRequest request) {
        BudgetFund fund = new BudgetFund();
        fund.setName(request.getName());
        fund.setTotalAmount(request.getTotalAmount());
        fund.setUsedAmount(BigDecimal.ZERO);
        fund.setFiscalYear(request.getFiscalYear());
        budgetFundMapper.insert(fund);
        log.info("预算资金创建成功: id={}, name={}", fund.getId(), fund.getName());
        return convertToResponse(fund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BudgetFundResponse updateFund(Long id, BudgetFundRequest request) {
        BudgetFund fund = budgetFundMapper.selectById(id);
        if (fund == null || fund.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BUDGET_FUND_NOT_FOUND, "预算资金不存在");
        }
        fund.setName(request.getName());
        fund.setTotalAmount(request.getTotalAmount());
        fund.setFiscalYear(request.getFiscalYear());
        budgetFundMapper.updateById(fund);
        log.info("预算资金更新成功: id={}", id);
        return convertToResponse(fund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFund(Long id) {
        BudgetFund fund = budgetFundMapper.selectById(id);
        if (fund == null || fund.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BUDGET_FUND_NOT_FOUND, "预算资金不存在");
        }
        budgetFundMapper.deleteById(id);
        log.info("预算资金删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BudgetFundResponse allocateToOrder(Long fundId, Long orderId, BigDecimal amount) {
        BudgetFund fund = budgetFundMapper.selectById(fundId);
        if (fund == null || fund.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BUDGET_FUND_NOT_FOUND, "预算资金不存在");
        }
        BigDecimal remaining = fund.getTotalAmount().subtract(fund.getUsedAmount());
        if (remaining.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.BUDGET_INSUFFICIENT_BALANCE, "预算余额不足，剩余: " + remaining);
        }
        fund.setUsedAmount(fund.getUsedAmount().add(amount));
        budgetFundMapper.updateById(fund);
        log.info("预算分配成功: fundId={}, orderId={}, amount={}", fundId, orderId, amount);
        return convertToResponse(fund);
    }

    private BudgetFundResponse convertToResponse(BudgetFund fund) {
        BigDecimal remaining = fund.getTotalAmount().subtract(
                fund.getUsedAmount() != null ? fund.getUsedAmount() : BigDecimal.ZERO);
        return BudgetFundResponse.builder()
                .id(fund.getId())
                .name(fund.getName())
                .totalAmount(fund.getTotalAmount())
                .usedAmount(fund.getUsedAmount() != null ? fund.getUsedAmount() : BigDecimal.ZERO)
                .remaining(remaining)
                .fiscalYear(fund.getFiscalYear())
                .createTime(fund.getCreateTime())
                .updateTime(fund.getUpdateTime())
                .build();
    }
}
