package com.library.system.controller;

import com.library.system.dto.ApiResponse;
import com.library.system.dto.NotificationResponse;
import com.library.system.dto.PageResult;
import com.library.system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "用户通知的查询、标记已读和删除")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @Operation(summary = "获取通知列表", description = "分页查询当前用户的通知列表")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<NotificationResponse>> listNotifications(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        PageResult<NotificationResponse> result = notificationService.listNotifications(userId, current, size, type, status);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取未读通知数量", description = "查询当前用户的未读通知数量")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> getUnreadCount(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Long count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(count);
    }

    @Operation(summary = "标记通知已读", description = "将指定通知标记为已读")
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAsRead(
            @Parameter(description = "通知ID") @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        notificationService.markAsRead(userId, id);
        return ApiResponse.success("标记已读成功", null);
    }

    @Operation(summary = "全部标记已读", description = "将当前用户所有未读通知标记为已读")
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllAsRead(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        notificationService.markAllAsRead(userId);
        return ApiResponse.success("全部标记已读成功", null);
    }

    @Operation(summary = "删除通知", description = "删除指定通知")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteNotification(
            @Parameter(description = "通知ID") @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        notificationService.deleteNotification(userId, id);
        return ApiResponse.success("删除成功", null);
    }
}
