package com.library.system.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Token黑名单实体（存储在Redis中）
 * 用于实现Token失效机制
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Token JTI（唯一标识）
     */
    private String jti;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Token类型：ACCESS-访问令牌，REFRESH-刷新令牌
     */
    private String tokenType;

    /**
     * Token过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 加入黑名单时间
     */
    private LocalDateTime blacklistTime;

    /**
     * 失效原因：LOGOUT-用户登出，REFRESH-Token刷新，REVOKE-强制失效
     */
    private String reason;
}
