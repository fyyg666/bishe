package com.library.system.controller;

import com.library.system.dto.*;
import com.library.system.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 读者控制器
 * <p>
 * 处理读者的CRUD和管理操作，业务逻辑委托给 {@link ReaderService}。
 * 管理操作需要ADMIN或LIBRARIAN角色权限。
 * FIXED: QUAL-005 内部DTO类已提取为独立文件
 * FIXED: ARCH-003 移除直接注入UserMapper，改用ReaderService.findByUsername
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/readers")
@RequiredArgsConstructor
@Tag(name = "读者管理", description = "读者信息的增删改查和管理操作")
@SecurityRequirement(name = "bearerAuth")
public class ReaderController {

    private final ReaderService readerService;

    /**
     * 获取读者列表
     */
    @Operation(summary = "获取读者列表", description = "分页查询读者列表（需要管理员或图书管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<ReaderResponse>> listReaders(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "关键词搜索（用户名/姓名）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "角色筛选")
            @RequestParam(required = false) String role) {
        log.debug("查询读者列表: current={}, size={}, keyword={}, role={}",
                current, size, keyword, role);
        return ApiResponse.success(readerService.listReaders(current, size, keyword, role));
    }

    /**
     * 获取读者详情
     */
    @Operation(summary = "获取读者详情", description = "根据ID查询读者详细信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "读者不存在")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<ReaderResponse> getReaderById(
            @Parameter(description = "读者ID", required = true)
            @PathVariable Long id) {
        log.debug("查询读者详情: id={}", id);
        return ApiResponse.success(readerService.getReaderById(id));
    }

    /**
     * 获取当前读者信息
     */
    @Operation(summary = "获取当前读者信息", description = "获取当前登录用户的个人信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/me")
    public ApiResponse<ReaderResponse> getCurrentReader(
            Authentication authentication) {
        log.debug("查询当前读者信息");
        return ApiResponse.success(readerService.getCurrentReader(authentication.getName()));
    }

    /**
     * 注册读者
     */
    @Operation(summary = "注册读者", description = "注册新读者账号")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "用户名已存在或参数错误")
    })
    @PostMapping
    public ApiResponse<ReaderResponse> registerReader(
            @Parameter(description = "注册请求体", required = true)
            @RequestBody @Valid ReaderRegisterRequest request) {
        log.info("注册新读者: {}", request.getUsername());
        ReaderResponse response = readerService.registerReader(
                request.getUsername(), request.getPassword(),
                request.getRealName(), request.getPhone(), request.getEmail());
        return ApiResponse.success("读者注册成功", response);
    }

    /**
     * 更新读者信息
     */
    @Operation(summary = "更新读者信息", description = "更新读者信息（普通用户可更新自己的，管理员可更新任意用户）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权修改该用户信息"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReaderResponse> updateReader(
            @Parameter(description = "读者ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "更新请求体", required = true)
            @RequestBody @Valid ReaderUpdateRequest request,
            Authentication authentication) {
        log.info("更新读者信息: id={}", id);

        String username = authentication.getName();
        Long currentUserId = readerService.getUserIdByUsername(username);
        boolean isAdmin = readerService.isCurrentUserAdmin(username);

        ReaderResponse response = readerService.updateReader(
                id, currentUserId, isAdmin,
                request.getRealName(), request.getPhone(), request.getEmail(), request.getAvatar(),
                request.getRole(), request.getStatus(), request.getCreditScore(), request.getMaxBorrowCount());
        return ApiResponse.success("读者信息更新成功", response);
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改用户密码（需要验证旧密码）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "密码修改成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "旧密码错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权修改该用户密码")
    })
    @PostMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(
            @Parameter(description = "读者ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "密码修改请求体", required = true)
            @RequestBody @Valid PasswordChangeRequest request,
            Authentication authentication) {
        log.info("修改密码: id={}", id);

        String username = authentication.getName();
        Long currentUserId = readerService.getUserIdByUsername(username);

        readerService.changePassword(id, currentUserId,
                request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success("密码修改成功", null);
    }

    /**
     * 删除读者
     */
    @Operation(summary = "删除读者", description = "删除读者账号（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权删除"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "读者不存在")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @CacheEvict(value = "readers", allEntries = true)
    public ApiResponse<Void> deleteReader(
            @Parameter(description = "读者ID", required = true)
            @PathVariable Long id) {
        log.info("删除读者: id={}", id);
        readerService.deleteReader(id);
        return ApiResponse.success("读者删除成功", null);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "重置读者密码为默认密码（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "重置成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权重置"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "读者不存在")
    })
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Void> resetPassword(
            @Parameter(description = "读者ID", required = true)
            @PathVariable Long id) {
        log.info("重置读者密码: id={}", id);
        readerService.resetPassword(id);
        return ApiResponse.success("密码已重置为默认密码", null);
    }

    /**
     * 更新读者状态
     */
    @Operation(summary = "更新读者状态", description = "禁用或启用读者账号（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "状态更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权操作"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "读者不存在")
    })
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @CacheEvict(value = "readers", allEntries = true)
    public ApiResponse<Void> updateReaderStatus(
            @Parameter(description = "读者ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "是否禁用", required = true)
            @RequestParam Boolean disabled) {
        log.info("更新读者状态: id={}, disabled={}", id, disabled);
        readerService.updateReaderStatus(id, disabled);
        return ApiResponse.success(disabled ? "读者账户已禁用" : "读者账户已启用", null);
    }
}
