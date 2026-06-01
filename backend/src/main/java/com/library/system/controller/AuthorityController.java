package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.AuthorityRecordRequest;
import com.library.system.dto.AuthorityRecordResponse;
import com.library.system.dto.PageResult;
import com.library.system.service.AuthorityService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/authorities")
@RequiredArgsConstructor
@Tag(name = "规范控制管理", description = "规范记录的CRUD与检索")
@SecurityRequirement(name = "bearerAuth")
public class AuthorityController extends BaseController {

    private final AuthorityService authorityService;

    @Operation(summary = "查询规范记录列表", description = "按类型和关键词分页查询规范记录")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<AuthorityRecordResponse>> listAuthorities(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String authorityType,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(authorityService.listAuthorities(current, size, authorityType, keyword));
    }

    @Operation(summary = "获取规范记录详情", description = "根据ID查询规范记录详细信息")
    @GetMapping("/{id}")
    public ApiResponse<AuthorityRecordResponse> getAuthority(@PathVariable Long id) {
        return ApiResponse.success(authorityService.getAuthority(id));
    }

    @Operation(summary = "创建规范记录", description = "创建新的规范记录")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<AuthorityRecordResponse> createAuthority(@Valid @RequestBody AuthorityRecordRequest request) {
        return ApiResponse.success("创建成功", authorityService.createAuthority(request));
    }

    @Operation(summary = "更新规范记录", description = "更新规范记录信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<AuthorityRecordResponse> updateAuthority(
            @PathVariable Long id,
            @Valid @RequestBody AuthorityRecordRequest request) {
        return ApiResponse.success("更新成功", authorityService.updateAuthority(id, request));
    }

    @Operation(summary = "删除规范记录", description = "删除规范记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteAuthority(@PathVariable Long id) {
        authorityService.deleteAuthority(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "检索规范标目", description = "按类型和标目关键词检索，用于MARC编辑器自动补全")
    @GetMapping("/search")
    public ApiResponse<List<AuthorityRecordResponse>> searchByHeading(
            @RequestParam(required = false) String authorityType,
            @RequestParam String keyword) {
        return ApiResponse.success(authorityService.searchByHeading(authorityType, keyword));
    }
}
