package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Vendor;
import com.library.system.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
@Tag(name = "供应商管理", description = "供应商的增删改查操作")
@SecurityRequirement(name = "bearerAuth")
public class VendorController extends BaseController {

    private final VendorService vendorService;

    @Operation(summary = "获取供应商列表", description = "分页查询供应商列表")
    @GetMapping
    public ApiResponse<PageResult<Vendor>> listVendors(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "关键词搜索")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选")
            @RequestParam(required = false) String status) {
        log.debug("查询供应商列表: current={}, size={}, keyword={}, status={}", current, size, keyword, status);
        return ApiResponse.success(vendorService.listVendors(current, size, keyword, status));
    }

    @Operation(summary = "获取供应商详情", description = "根据ID查询供应商详情")
    @GetMapping("/{id}")
    public ApiResponse<Vendor> getVendorById(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id) {
        log.debug("查询供应商详情: id={}", id);
        return ApiResponse.success(vendorService.getVendorById(id));
    }

    @Operation(summary = "新增供应商", description = "创建新供应商（需要管理员权限）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Vendor> createVendor(@RequestBody Vendor vendor) {
        log.info("新增供应商: {}", vendor.getName());
        return ApiResponse.success("供应商创建成功", vendorService.createVendor(vendor));
    }

    @Operation(summary = "更新供应商", description = "更新供应商信息（需要管理员权限）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Vendor> updateVendor(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id,
            @RequestBody Vendor vendor) {
        log.info("更新供应商: id={}", id);
        return ApiResponse.success("供应商更新成功", vendorService.updateVendor(id, vendor));
    }

    @Operation(summary = "删除供应商", description = "删除供应商（需要管理员权限）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> deleteVendor(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id) {
        log.info("删除供应商: id={}", id);
        vendorService.deleteVendor(id);
        return ApiResponse.success("供应商删除成功", null);
    }
}
