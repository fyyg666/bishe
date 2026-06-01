package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.BudgetFundRequest;
import com.library.system.dto.BudgetFundResponse;
import com.library.system.service.BudgetFundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/budget-funds")
@RequiredArgsConstructor
@Tag(name = "预算管理", description = "图书馆预算资金管理")
@SecurityRequirement(name = "bearerAuth")
public class BudgetFundController extends BaseController {

    private final BudgetFundService budgetFundService;

    @Operation(summary = "获取预算列表", description = "按财政年度查询预算资金列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<List<BudgetFundResponse>> listFunds(
            @RequestParam(required = false) Integer fiscalYear) {
        return ApiResponse.success(budgetFundService.listFunds(fiscalYear));
    }

    @Operation(summary = "获取预算详情", description = "查询预算资金详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BudgetFundResponse> getFund(@PathVariable Long id) {
        return ApiResponse.success(budgetFundService.getFund(id));
    }

    @Operation(summary = "创建预算", description = "创建预算资金（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BudgetFundResponse> createFund(
            @Valid @RequestBody BudgetFundRequest request) {
        log.info("创建预算资金: name={}, fiscalYear={}", request.getName(), request.getFiscalYear());
        return ApiResponse.success("预算创建成功", budgetFundService.createFund(request));
    }

    @Operation(summary = "更新预算", description = "更新预算资金（需要管理员权限）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BudgetFundResponse> updateFund(
            @PathVariable Long id,
            @Valid @RequestBody BudgetFundRequest request) {
        return ApiResponse.success("预算更新成功", budgetFundService.updateFund(id, request));
    }

    @Operation(summary = "删除预算", description = "删除预算资金（需要管理员权限）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<String> deleteFund(@PathVariable Long id) {
        budgetFundService.deleteFund(id);
        return ApiResponse.success("success", "预算已删除");
    }

    @Operation(summary = "分配预算到订单", description = "将预算资金分配到采购订单（需要管理员权限）")
    @PostMapping("/{fundId}/allocate")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BudgetFundResponse> allocateToOrder(
            @PathVariable Long fundId,
            @RequestParam Long orderId,
            @RequestParam BigDecimal amount) {
        return ApiResponse.success("预算分配成功", budgetFundService.allocateToOrder(fundId, orderId, amount));
    }
}
