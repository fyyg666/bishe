package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.PurchaseSuggestionRequest;
import com.library.system.dto.PurchaseSuggestionResponse;

public interface PurchaseSuggestionService {

    PurchaseSuggestionResponse createSuggestion(Long userId, PurchaseSuggestionRequest request);

    PageResult<PurchaseSuggestionResponse> listSuggestions(Long current, Long size, String status);

    PageResult<PurchaseSuggestionResponse> getMySuggestions(Long userId, Long current, Long size);

    PurchaseSuggestionResponse approveSuggestion(Long id, Long reviewerId, String remark);

    PurchaseSuggestionResponse rejectSuggestion(Long id, Long reviewerId, String remark);
}
