package com.library.system.controller;

import com.library.system.dto.*;
import com.library.system.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 信用积分控制器
 * <p>
 * 处理用户信用积分的查询和积分变动日志的查看。
 * 普通用户仅可查看自己的积分和日志，管理员可查看任意用户的积分信息。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/credits")
@RequiredArgsConstructor
@Tag(name = "信用管理", description = "用户信用积分查询和积分变动日志查看")
@SecurityRequirement(name = "bearerAuth")
public class CreditController extends BaseController { // FIXED: ARCH-002 继承BaseController

    private final CreditService creditService;

    /**
     * 获取当前用户积分
     */
    @Operation(summary = "获取我的积分", description = "查询当前登录用户的信用积分")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Integer.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<Integer> getMyCredit(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.debug("查询我的积分: userId={}", userId);
        Integer credit = creditService.getUserCredit(userId);
        return ApiResponse.success(credit);
    }

    /**
     * 获取当前用户信用等级
     */
    @Operation(summary = "获取我的信用等级", description = "查询当前登录用户的信用等级信息（积分、等级名称、下一等级所需积分）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/level")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<CreditLevelResponse> getMyLevel(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.debug("查询我的信用等级: userId={}", userId);
        CreditLevelResponse level = creditService.getUserLevel(userId);
        return ApiResponse.success(level);
    }

    /**
     * 获取指定用户积分
     */
    @Operation(summary = "获取用户积分", description = "根据ID查询用户信用积分（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<Integer> getUserCredit(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.debug("查询用户积分: userId={}", userId);
        Integer credit = creditService.getUserCredit(userId);
        return ApiResponse.success(credit);
    }

    /**
     * 获取我的积分日志
     */
    @Operation(summary = "获取我的积分日志", description = "分页查询当前用户的积分变动日志")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('READER', 'ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<CreditLogResponse>> getMyCreditLogs(
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        log.debug("查询我的积分日志: userId={}, current={}, size={}", userId, current, size);
        PageResult<CreditLogResponse> result = creditService.getCreditLogs(userId, current, size);
        return ApiResponse.success(result);
    }

    /**
     * 获取指定用户积分日志
     */
    @Operation(summary = "获取用户积分日志", description = "分页查询指定用户的积分变动日志（需要管理员权限）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ApiResponse<PageResult<CreditLogResponse>> getUserCreditLogs(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "当前页（默认1）")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小（默认10）")
            @RequestParam(defaultValue = "10") Long size) {
        log.debug("查询用户积分日志: userId={}, current={}, size={}", userId, current, size);
        PageResult<CreditLogResponse> result = creditService.getCreditLogs(userId, current, size);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取积分规则", description = "获取所有积分规则列表")
    @GetMapping("/rules")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<java.util.List<CreditRuleResponse>> getCreditRules() {
        return ApiResponse.success(creditService.getCreditRules());
    }
}
