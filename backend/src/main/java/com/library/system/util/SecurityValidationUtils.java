package com.library.system.util;

import com.library.system.common.Constants;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.UserMapper;
import com.library.system.security.SecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 安全验证工具类
 * <p>
 * 封装常见的权限检查和用户验证操作，消除Service层中的重复权限检查代码。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>验证用户是否存在且未被禁用</li>
 *   <li>检查用户是否有权操作指定资源</li>
 *   <li>验证用户角色权限</li>
 * </ul>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2026-04-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityValidationUtils {

    private final UserMapper userMapper;
    private final SecurityAuditLogger securityAuditLogger;

    /**
     * 验证用户存在且未被禁用
     *
     * @param userId 用户ID
     * @return User 实体
     * @throws ResourceNotFoundException 用户不存在
     * @throws ForbiddenException 用户已被禁用
     */
    public User validateUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在");
        }
        if (Constants.UserStatus.DISABLED.equals(user.getStatus())) {
            log.warn("用户已被禁用: userId={}, username={}", userId, user.getUsername());
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "用户已被禁用");
        }
        return user;
    }

    /**
     * 验证用户是否有权操作指定资源（检查资源归属）
     *
     * @param operatorId 操作者ID
     * @param resourceUserId 资源所属用户ID
     * @param resourceName 资源名称（用于日志）
     * @throws ForbiddenException 无权操作此资源
     */
    public void validateResourceOwnership(Long operatorId, Long resourceUserId, String resourceName) {
        if (!operatorId.equals(resourceUserId)) {
            log.warn("无权操作{}: operatorId={}, resourceUserId={}", 
                    resourceName, operatorId, resourceUserId);
            securityAuditLogger.logAccessDenied(operatorId, resourceName, "OWNER");
            throw new ForbiddenException(
                    ErrorCode.INSUFFICIENT_PERMISSION, 
                    "无权操作此" + resourceName);
        }
    }

    /**
     * 验证用户是否具有指定角色
     *
     * @param user User 实体
     * @param requiredRole 需要的角色
     * @throws ForbiddenException 角色权限不足
     */
    public void validateRole(User user, String requiredRole) {
        if (!requiredRole.equals(user.getRole())) {
            log.warn("角色权限不足: userId={}, userRole={}, requiredRole={}", 
                    user.getId(), user.getRole(), requiredRole);
            securityAuditLogger.logAccessDenied(user.getId(), "role_check", requiredRole);
            throw new ForbiddenException(
                    ErrorCode.INSUFFICIENT_PERMISSION, 
                    "需要" + requiredRole + "权限");
        }
    }

    /**
     * 验证用户是否具有任意指定角色之一
     *
     * @param user User 实体
     * @param requiredRoles 需要的角色数组
     * @throws ForbiddenException 角色权限不足
     */
    public void validateAnyRole(User user, String... requiredRoles) {
        for (String role : requiredRoles) {
            if (role.equals(user.getRole())) {
                return;
            }
        }
        log.warn("角色权限不足: userId={}, userRole={}, requiredRoles={}", 
                user.getId(), user.getRole(), String.join(", ", requiredRoles));
        throw new ForbiddenException(
                ErrorCode.INSUFFICIENT_PERMISSION, 
                "权限不足，需要以下角色之一: " + String.join(", ", requiredRoles));
    }

    /**
     * 验证当前操作用户与资源所属用户一致，或用户具有管理员角色
     *
     * @param operatorId 操作者ID
     * @param resourceUserId 资源所属用户ID
     * @param userMapper UserMapper（用于查询操作者角色）
     * @param resourceName 资源名称（用于日志）
     * @throws ForbiddenException 无权操作此资源
     */
    public void validateOwnershipOrAdmin(Long operatorId, Long resourceUserId, 
                                           String resourceName) {
        if (operatorId.equals(resourceUserId)) {
            return; // 资源所属用户，允许操作
        }

        // 检查是否为管理员
        User operator = userMapper.selectById(operatorId);
        if (operator != null && 
                (Constants.Role.ADMIN.equals(operator.getRole()) || Constants.Role.LIBRARIAN.equals(operator.getRole()))) {
            return; // 管理员或图书管理员，允许操作
        }

        log.warn("无权操作{}: operatorId={}, resourceUserId={}", 
                resourceName, operatorId, resourceUserId);
        securityAuditLogger.logAccessDenied(operatorId, resourceName, Constants.Role.ADMIN + "/" + Constants.Role.LIBRARIAN);
        throw new ForbiddenException(
                ErrorCode.INSUFFICIENT_PERMISSION, 
                "无权操作此" + resourceName);
    }
}
