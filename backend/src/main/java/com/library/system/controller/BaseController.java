package com.library.system.controller;

import org.springframework.security.core.Authentication;

/**
 * 基础控制器
 * FIXED: ARCH-002 提取公共的getUserIdFromAuthentication方法，
 * 避免BorrowController、SeatController、CreditController中重复定义
 *
 * @author Library Team
 * @version 2.0.0
 */
public abstract class BaseController {

    /**
     * 从认证信息中获取用户ID
     *
     * @param authentication Spring Security认证信息
     * @return 用户ID
     */
    protected Long getUserIdFromAuthentication(Authentication authentication) {
        return (Long) authentication.getDetails();
    }
}
