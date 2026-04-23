package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录响应DTO
 * 包含双Token和用户基本信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 访问令牌（短期有效）
     */
    private String accessToken;

    /**
     * 刷新令牌（长期有效）
     */
    private String refreshToken;

    /**
     * Token类型
     */
    private String tokenType;

    /**
     * Access Token过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 信用积分
     */
    private Integer creditScore;

    /**
     * 当前借阅数量
     */
    private Integer currentBorrowCount;

    /**
     * 最大可借数量
     */
    private Integer maxBorrowCount;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 用户详细信息
     */
    private UserInfo userInfo;

    /**
     * 用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 真实姓名
         */
        private String realName;

        /**
         * 角色
         */
        private String role;

        /**
         * 读者卡号
         */
        private String cardNumber;

        /**
         * 信用积分
         */
        private Integer creditScore;

        /**
         * 当前借阅数量
         */
        private Integer currentBorrows;

        /**
         * 头像URL
         */
        private String avatar;

        /**
         * 状态
         */
        private Integer status;

        /**
         * 手机号（脱敏）
         */
        private String phone;

        /**
         * 邮箱（脱敏）
         */
        private String email;

        /**
         * 创建时间
         */
        private String createTime;
    }
}
