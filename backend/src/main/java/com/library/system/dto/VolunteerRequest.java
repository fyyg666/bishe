package com.library.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 志愿服务请求DTO 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerRequest {

    /**
     * 服务日期
     */
    @NotNull(message = "服务日期不能为空")
    private LocalDateTime serviceDate;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 服务时长（小时）
     */
    @DecimalMin(value = "0.5", message = "服务时长不能小于0.5小时")
    @DecimalMax(value = "12.0", message = "单次服务时长不能超过12小时")
    private BigDecimal serviceHours;

    /**
     * 服务类型
     */
    @NotBlank(message = "服务类型不能为空")
    @Size(max = 50, message = "服务类型长度不能超过50个字符")
    private String serviceType;

    /**
     * 服务描述
     */
    @Size(max = 500, message = "服务描述长度不能超过500个字符")
    private String description;
}
