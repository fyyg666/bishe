package com.library.system.service;

import com.library.system.dto.BorrowRuleRequest;
import com.library.system.dto.BorrowRuleResponse;
import com.library.system.entity.BorrowRule;

import java.util.List;

public interface BorrowRuleService {

    List<BorrowRuleResponse> listRules();

    BorrowRuleResponse getRuleByType(String readerType, String bookType);

    BorrowRule getRuleEntity(String readerType, String bookType);

    BorrowRuleResponse createRule(BorrowRuleRequest request);

    BorrowRuleResponse updateRule(Long id, BorrowRuleRequest request);

    void deleteRule(Long id);
}
