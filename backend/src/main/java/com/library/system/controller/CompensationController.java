package com.library.system.controller;

import com.library.system.dto.CompensationRequest;
import com.library.system.dto.CompensationResponse;
import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.service.CompensationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 赔偿管理控制器
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/compensations")
@RequiredArgsConstructor
@Tag(name = "赔偿管理", description = "图书丢失/损坏赔偿管理")
@SecurityRequirement(name = "bearerAuth")
public class CompensationController extends BaseController {

    private final CompensationService compensationService;

    @Operation(summary = "创建赔偿订单", description = "创建图书丢失/损坏赔偿单（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<CompensationResponse> createCompensation(
            @Valid @RequestBody CompensationRequest request,
            Authentication authentication) {
        Long operatorId = getUserIdFromAuthentication(authentication);
        log.info("创建赔偿订单: userId={}, bookId={}", request.getUserId(), request.getBookId());
        return ApiResponse.success("赔偿订单创建成功",
                compensationService.createCompensation(request, operatorId));
    }

    @Operation(summary = "获取赔偿列表", description = "分页查询赔偿记录（需要管理员权限）")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<CompensationResponse>> listCompensations(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(compensationService.listCompensations(current, size, status));
    }

    @Operation(summary = "获取赔偿详情", description = "查询赔偿订单详情（需要管理员权限）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<CompensationResponse> getCompensation(@PathVariable Long id) {
        return ApiResponse.success(compensationService.getCompensationById(id));
    }

    @Operation(summary = "现金赔偿", description = "以现金方式处理赔偿（需要管理员权限）")
    @PostMapping("/{id}/pay/cash")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<CompensationResponse> processCash(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            Authentication authentication) {
        Long operatorId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success("现金赔偿处理成功",
                compensationService.processCashPayment(id, operatorId, remark));
    }

    @Operation(summary = "积分抵扣赔偿", description = "以积分抵扣方式处理赔偿（需要管理员权限）")
    @PostMapping("/{id}/pay/credit")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<CompensationResponse> processCredit(
            @PathVariable Long id,
            @RequestParam Integer creditAmount,
            @RequestParam(required = false) String remark,
            Authentication authentication) {
        Long operatorId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success("积分抵扣处理成功",
                compensationService.processCreditPayment(id, operatorId, creditAmount, remark));
    }

    @Operation(summary = "志愿服务抵扣赔偿", description = "以志愿服务时长抵扣赔偿（需要管理员权限）")
    @PostMapping("/{id}/pay/volunteer")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<CompensationResponse> processVolunteer(
            @PathVariable Long id,
            @RequestParam BigDecimal hours,
            @RequestParam(required = false) String remark,
            Authentication authentication) {
        Long operatorId = getUserIdFromAuthentication(authentication);
        return ApiResponse.success("志愿服务抵扣处理成功",
                compensationService.processVolunteerPayment(id, operatorId, hours, remark));
    }

    @Operation(summary = "取消赔偿订单", description = "取消待处理的赔偿订单（需要管理员权限）")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<String> cancelCompensation(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {
        Long operatorId = getUserIdFromAuthentication(authentication);
        compensationService.cancelCompensation(id, operatorId, reason);
        return ApiResponse.success("success", "赔偿订单已取消");
    }
}
