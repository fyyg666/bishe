package com.library.system.service;

import com.library.system.dto.BudgetFundRequest;
import com.library.system.dto.BudgetFundResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetFundService {

    List<BudgetFundResponse> listFunds(Integer fiscalYear);

    BudgetFundResponse getFund(Long id);

    BudgetFundResponse createFund(BudgetFundRequest request);

    BudgetFundResponse updateFund(Long id, BudgetFundRequest request);

    void deleteFund(Long id);

    BudgetFundResponse allocateToOrder(Long fundId, Long orderId, BigDecimal amount);
}
