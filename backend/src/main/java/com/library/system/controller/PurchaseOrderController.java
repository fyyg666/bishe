package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.dto.PurchaseOrderRequest;
import com.library.system.dto.PurchaseOrderResponse;
import com.library.system.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "采购管理", description = "图书采购订单管理")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseOrderController extends BaseController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "创建采购订单")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseOrderResponse> createOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.success("采购订单创建成功", purchaseOrderService.createOrder(request));
    }

    @Operation(summary = "更新采购订单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseOrderResponse> updateOrder(
            @PathVariable Long id, @Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.success("采购订单更新成功", purchaseOrderService.updateOrder(id, request));
    }

    @Operation(summary = "获取采购订单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseOrderResponse> getOrder(@PathVariable Long id) {
        return ApiResponse.success(purchaseOrderService.getOrder(id));
    }

    @Operation(summary = "删除采购订单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<String> deleteOrder(@PathVariable Long id) {
        purchaseOrderService.deleteOrder(id);
        return ApiResponse.success("success", "采购订单已删除");
    }

    @Operation(summary = "分页查询采购订单")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<PurchaseOrderResponse>> listOrders(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(purchaseOrderService.listOrders(current, size, status));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseOrderResponse> submitForApproval(@PathVariable Long id) {
        return ApiResponse.success("已提交审批", purchaseOrderService.submitForApproval(id));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> approveOrder(@PathVariable Long id) {
        return ApiResponse.success("审批通过", purchaseOrderService.approveOrder(id));
    }

    @Operation(summary = "收货登记")
    @PostMapping("/{orderId}/items/{itemId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseOrderResponse> receiveItems(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam int receivedQty) {
        return ApiResponse.success("收货登记成功", purchaseOrderService.receiveItems(orderId, itemId, receivedQty));
    }

    @Operation(summary = "取消采购订单")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PurchaseOrderResponse> cancelOrder(@PathVariable Long id) {
        return ApiResponse.success("订单已取消", purchaseOrderService.cancelOrder(id));
    }
}
