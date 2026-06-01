package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.BorrowRuleRequest;
import com.library.system.dto.BorrowRuleResponse;
import com.library.system.service.BorrowRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/borrow-rules")
@RequiredArgsConstructor
@Tag(name = "借阅规则", description = "借阅规则的增删改查")
@SecurityRequirement(name = "bearerAuth")
public class BorrowRuleController extends BaseController {

    private final BorrowRuleService borrowRuleService;

    @Operation(summary = "获取借阅规则列表", description = "查询所有借阅规则（需要管理员权限）")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<BorrowRuleResponse>> listRules() {
        return ApiResponse.success(borrowRuleService.listRules());
    }

    @Operation(summary = "按类型查询借阅规则", description = "根据读者类型和图书类型查询借阅规则")
    @GetMapping("/query")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BorrowRuleResponse> getRuleByType(
            @Parameter(description = "读者类型") @RequestParam String readerType,
            @Parameter(description = "图书类型") @RequestParam(defaultValue = "NORMAL") String bookType) {
        return ApiResponse.success(borrowRuleService.getRuleByType(readerType, bookType));
    }

    @Operation(summary = "创建借阅规则", description = "创建新的借阅规则（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<BorrowRuleResponse> createRule(@Valid @RequestBody BorrowRuleRequest request) {
        return ApiResponse.success("规则创建成功", borrowRuleService.createRule(request));
    }

    @Operation(summary = "更新借阅规则", description = "更新借阅规则（需要管理员权限）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<BorrowRuleResponse> updateRule(
            @PathVariable Long id, @Valid @RequestBody BorrowRuleRequest request) {
        return ApiResponse.success("规则更新成功", borrowRuleService.updateRule(id, request));
    }

    @Operation(summary = "删除借阅规则", description = "删除借阅规则（需要管理员权限）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        borrowRuleService.deleteRule(id);
        return ApiResponse.success("规则删除成功", null);
    }
}
