package com.library.system.controller;

import com.library.system.common.Constants;
import com.library.system.dto.*;
import com.library.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>
 * 处理用户登录、注册、Token刷新和登出等认证相关操作。
 * 提供基于JWT的双Token认证机制（Access Token + Refresh Token）。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、Token刷新和登出等认证相关操作")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回双Token（Access Token + Refresh Token）")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "用户名或密码错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "账户已被锁定")
    })
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Parameter(description = "登录请求体（包含用户名和密码）", required = true)
            @Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "用户名已存在或参数错误")
    })
    @PostMapping("/register")
    public ApiResponse<LoginResponse.UserInfo> register(
            @Parameter(description = "注册请求体（包含用户名、密码等信息）", required = true)
            @Valid @RequestBody RegisterRequest request) {
        log.info("用户注册请求: {}", request.getUsername());
        LoginResponse.UserInfo userInfo = authService.register(request);
        return ApiResponse.success("注册成功", userInfo);
    }

    /**
     * 刷新Token
     */
    @Operation(summary = "刷新Token", description = "使用Refresh Token获取新的Access Token")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token刷新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token无效或已过期")
    })
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(
            @Parameter(description = "刷新Token请求体", required = true)
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token刷新请求");
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success("Token刷新成功", response);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "使当前Access Token失效")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登出成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "无效的Token")
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Parameter(description = "HTTP请求（包含Authorization头）", required = true)
            HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(Constants.Token.BEARER_PREFIX)) { 
            String accessToken = authHeader.substring(Constants.Token.BEARER_PREFIX.length()).trim();
            if (accessToken.isEmpty()) {
                return ApiResponse.error(400, "无效的Token");
            }
            authService.logout(accessToken);
            log.info("用户登出成功");
            return ApiResponse.success("登出成功", null);
        }
        return ApiResponse.error(400, "无效的Token");
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据Token获取当前登录用户的详细信息")
    @GetMapping("/info")
    public ApiResponse<LoginResponse.UserInfo> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.error(401, "未登录");
        }
        try {
            Long userId = Long.valueOf(authentication.getPrincipal().toString());
            LoginResponse.UserInfo userInfo = authService.getCurrentUser(userId);
            return ApiResponse.success("获取成功", userInfo);
        } catch (NumberFormatException e) {
            log.error("从认证信息解析用户ID失败: principal={}", authentication.getPrincipal());
            return ApiResponse.error(400, "无效的用户认证信息");
        }
    }
}
