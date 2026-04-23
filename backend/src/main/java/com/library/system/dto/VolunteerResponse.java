package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 志愿服务响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 服务日期
     */
    private LocalDateTime serviceDate;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 服务时长（小时）
     */
    private BigDecimal serviceHours;

    /**
     * 服务类型
     */
    private String serviceType;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核人姓名
     */
    private String reviewerName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 审核备注
     */
    private String reviewRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
