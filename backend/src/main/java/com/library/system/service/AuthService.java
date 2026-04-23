package com.library.system.service;

import com.library.system.dto.*;

/**
 * 认证服务接口
 * <p>
 * 处理用户登录、注册、Token刷新和登出等核心认证操作。
 * 基于JWT实现双Token认证机制（Access Token + Refresh Token），
 * 支持密码修改、Token校验和当前用户信息获取。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含双Token）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册成功的用户信息
     */
    LoginResponse.UserInfo register(RegisterRequest request);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新Token字符串
     * @return 新的Token对
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     *
     * @param token 访问令牌
     */
    void logout(String token);

    /**
     * 获取当前用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    LoginResponse.UserInfo getCurrentUser(Long userId);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 验证Token是否有效
     *
     * @param token Token字符串
     * @return 是否有效
     */
    boolean validateToken(String token);
}
