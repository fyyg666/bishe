package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.dto.PurchaseSuggestionRequest;
import com.library.system.dto.PurchaseSuggestionResponse;
import com.library.system.service.PurchaseSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/purchase-suggestions")
@RequiredArgsConstructor
@Tag(name = "荐购管理", description = "图书荐购建议管理")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseSuggestionController extends BaseController {

    private final PurchaseSuggestionService purchaseSuggestionService;

    @Operation(summary = "提交荐购建议", description = "已认证用户提交荐购建议")
    @PostMapping
    public ApiResponse<PurchaseSuggestionResponse> createSuggestion(
            @Valid @RequestBody PurchaseSuggestionRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("提交荐购建议: userId={}, title={}", userId, request.getTitle());
        return ApiResponse.success("荐购建议提交成功",
                purchaseSuggestionService.createSuggestion(userId, request));
    }

    @Operation(summary = "获取荐购列表", description = "分页查询荐购建议（管理员/馆员）")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<PurchaseSuggestionResponse>> listSuggestions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(purchaseSuggestionService.listSuggestions(current, size, status));
    }

    @Operation(summary = "我的荐购", description = "查询当前用户的荐购建议")
    @GetMapping("/my")
    public ApiResponse<PageResult<PurchaseSuggestionResponse>> mySuggestions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success(purchaseSuggestionService.getMySuggestions(userId, current, size));
    }

    @Operation(summary = "批准荐购", description = "批准荐购建议（管理员/馆员）")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseSuggestionResponse> approveSuggestion(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            Authentication authentication) {
        Long reviewerId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success("荐购建议已批准",
                purchaseSuggestionService.approveSuggestion(id, reviewerId, remark));
    }

    @Operation(summary = "拒绝荐购", description = "拒绝荐购建议（管理员/馆员）")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseSuggestionResponse> rejectSuggestion(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            Authentication authentication) {
        Long reviewerId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success("荐购建议已拒绝",
                purchaseSuggestionService.rejectSuggestion(id, reviewerId, remark));
    }
}
