package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.dto.UnifiedSearchResponse;
import com.library.system.service.UnifiedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "统一检索", description = "跨资源类型统一检索")
@SecurityRequirement(name = "bearerAuth")
public class UnifiedSearchController extends BaseController {

    private final UnifiedSearchService unifiedSearchService;

    @Operation(summary = "统一检索", description = "跨图书和数字资源统一检索，resourceType: ALL/PRINT/DIGITAL")
    @GetMapping
    public ApiResponse<PageResult<UnifiedSearchResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String resourceType,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        log.debug("统一检索: keyword={}, resourceType={}, current={}, size={}", keyword, resourceType, current, size);
        PageResult<UnifiedSearchResponse> result = unifiedSearchService.search(keyword, resourceType, current, size);
        return ApiResponse.success(result);
    }
}
