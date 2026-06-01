package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.dto.SerialClaimRequest;
import com.library.system.dto.SerialClaimResponse;
import com.library.system.service.SerialClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/serial/claims")
@RequiredArgsConstructor
@Tag(name = "期刊催缺管理", description = "期刊催缺记录的创建、查询、处理与关闭")
@SecurityRequirement(name = "bearerAuth")
public class SerialClaimController extends BaseController {

    private final SerialClaimService claimService;

    @Operation(summary = "获取催缺列表", description = "分页查询催缺记录，支持按状态和类型筛选")
    @GetMapping
    public ApiResponse<PageResult<SerialClaimResponse>> listClaims(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "催缺状态筛选")
            @RequestParam(required = false) String claimStatus,
            @Parameter(description = "催缺类型筛选")
            @RequestParam(required = false) String claimType) {
        log.debug("查询催缺列表: current={}, size={}, claimStatus={}, claimType={}", current, size, claimStatus, claimType);
        return ApiResponse.success(claimService.listClaims(current, size, claimStatus, claimType));
    }

    @Operation(summary = "获取催缺详情", description = "根据ID查询催缺记录详情")
    @GetMapping("/{id}")
    public ApiResponse<SerialClaimResponse> getClaim(
            @Parameter(description = "催缺记录ID", required = true)
            @PathVariable Long id) {
        log.debug("查询催缺详情: id={}", id);
        return ApiResponse.success(claimService.getClaim(id));
    }

    @Operation(summary = "新建催缺", description = "创建催缺记录（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialClaimResponse> createClaim(
            @RequestBody SerialClaimRequest request,
            Authentication authentication) {
        Long operatorId = getUserIdFromAuthentication(authentication);
        log.info("新建催缺: subscriptionId={}, operatorId={}", request.getSubscriptionId(), operatorId);
        return ApiResponse.success("催缺记录创建成功", claimService.createClaim(request, operatorId));
    }

    @Operation(summary = "更新催缺", description = "更新催缺记录信息（需要管理员权限）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<SerialClaimResponse> updateClaim(
            @Parameter(description = "催缺记录ID", required = true)
            @PathVariable Long id,
            @RequestBody SerialClaimRequest request) {
        log.info("更新催缺记录: id={}", id);
        return ApiResponse.success("催缺记录更新成功", claimService.updateClaim(id, request));
    }

    @Operation(summary = "处理催缺", description = "处理催缺记录，设置处理结果（需要管理员权限）")
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> resolveClaim(
            @Parameter(description = "催缺记录ID", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String resolution = body.get("resolution");
        log.info("处理催缺记录: id={}", id);
        claimService.resolveClaim(id, resolution);
        return ApiResponse.success("催缺记录处理成功", null);
    }

    @Operation(summary = "关闭催缺", description = "关闭催缺记录（需要管理员权限）")
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> closeClaim(
            @Parameter(description = "催缺记录ID", required = true)
            @PathVariable Long id) {
        log.info("关闭催缺记录: id={}", id);
        claimService.closeClaim(id);
        return ApiResponse.success("催缺记录已关闭", null);
    }
}
