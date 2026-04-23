package com.library.system.controller;

import com.library.system.annotation.AuditLog;
import com.library.system.common.Constants;
import com.library.system.dto.*;
import com.library.system.service.AuthService;
import io.swagger.v3.oas.annotations.jakarta.Operation;
import io.swagger.v3.oas.annotations.jakarta.Parameter;
import io.swagger.v3.oas.annotations.jakarta.media.Content;
import io.swagger.v3.oas.annotations.jakarta.media.Schema;
import io.swagger.v3.oas.annotations.jakarta.responses.ApiResponse;
import io.swagger.v3.oas.annotations.jakarta.responses.ApiResponses;
import io.swagger.v3.oas.annotations.jakarta.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "423", description = "账户已被锁定")
    })
    @AuditLog(module = "认证管理", operation = "用户登录")
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "用户名已存在或参数错误")
    })
    @AuditLog(module = "认证管理", operation = "用户注册")
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token刷新成功"),
        @ApiResponse(responseCode = "401", description = "Refresh Token无效或已过期")
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登出成功"),
        @ApiResponse(responseCode = "400", description = "无效的Token")
    })
    @AuditLog(module = "认证管理", operation = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Parameter(description = "HTTP请求（包含Authorization头）", required = true)
            HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(Constants.Token.BEARER_PREFIX)) { 
            String accessToken = authHeader.substring(Constants.Token.BEARER_PREFIX.length()); 
            authService.logout(accessToken);
            log.info("用户登出成功");
            return ApiResponse.success("登出成功", null);
        }
        return ApiResponse.error(400, "无效的Token");
    }
}
