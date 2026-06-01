package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.BranchRequest;
import com.library.system.dto.BranchResponse;
import com.library.system.service.BranchService;
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
@RequestMapping("/branches")
@RequiredArgsConstructor
@Tag(name = "分馆管理", description = "分馆的增删改查操作")
@SecurityRequirement(name = "bearerAuth")
public class BranchController extends BaseController {

    private final BranchService branchService;

    @Operation(summary = "获取分馆列表", description = "查询所有分馆（扁平列表）")
    @GetMapping
    public ApiResponse<List<BranchResponse>> listBranches(
            @Parameter(description = "状态筛选: 1=启用, 0=停用")
            @RequestParam(required = false) String status) {
        return ApiResponse.success(branchService.listBranches(status));
    }

    @Operation(summary = "获取分馆树", description = "查询分馆树形结构")
    @GetMapping("/tree")
    public ApiResponse<List<BranchResponse>> listBranchTree() {
        return ApiResponse.success(branchService.listBranchTree());
    }

    @Operation(summary = "获取分馆详情", description = "根据ID查询分馆详情")
    @GetMapping("/{id}")
    public ApiResponse<BranchResponse> getBranch(
            @Parameter(description = "分馆ID", required = true)
            @PathVariable Long id) {
        return ApiResponse.success(branchService.getBranch(id));
    }

    @Operation(summary = "新增分馆", description = "创建新分馆（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        log.info("新增分馆: {}", request.getName());
        return ApiResponse.success("分馆创建成功", branchService.createBranch(request));
    }

    @Operation(summary = "更新分馆", description = "更新分馆信息（需要管理员权限）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<BranchResponse> updateBranch(
            @Parameter(description = "分馆ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody BranchRequest request) {
        log.info("更新分馆: id={}", id);
        return ApiResponse.success("分馆更新成功", branchService.updateBranch(id, request));
    }

    @Operation(summary = "删除分馆", description = "删除分馆（需要管理员权限）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteBranch(
            @Parameter(description = "分馆ID", required = true)
            @PathVariable Long id) {
        log.info("删除分馆: id={}", id);
        branchService.deleteBranch(id);
        return ApiResponse.success("分馆删除成功", null);
    }

    @Operation(summary = "获取子分馆", description = "根据父级ID查询子分馆列表")
    @GetMapping("/{parentId}/children")
    public ApiResponse<List<BranchResponse>> getSubBranches(
            @Parameter(description = "上级分馆ID", required = true)
            @PathVariable Long parentId) {
        return ApiResponse.success(branchService.getSubBranches(parentId));
    }
}
