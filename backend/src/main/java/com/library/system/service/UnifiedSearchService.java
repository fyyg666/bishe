package com.library.system.service;

import com.library.system.dto.UnifiedSearchResponse;
import com.library.system.dto.PageResult;

public interface UnifiedSearchService {
    PageResult<UnifiedSearchResponse> search(String keyword, String resourceType, Long current, Long size);
}
