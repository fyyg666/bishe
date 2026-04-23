package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 读者响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderResponse {

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
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 角色
     */
    private String role;

    /**
     * 状态
     */
    private String status;

    /**
     * 积分
     */
    private Integer creditScore;

    /**
     * 读者卡号
     */
    private String cardNumber;

    /**
     * 当前借阅数量
     */
    private Integer borrowCount;

    /**
     * 最大可借数量
     */
    private Integer maxBorrowCount;

    /**
     * 创建时间
     */
    private String createTime;
}
