package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 信用积分日志响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditLogResponse {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 积分变动值
     */
    private Integer changeValue;

    /**
     * 变动后积分余额
     */
    private Integer balance;

    /**
     * 变动类型
     */
    private String type;

    /**
     * 变动类型描述
     */
    private String typeDesc;

    /**
     * 变动描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
