package com.library.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 志愿服务统计DTO
 * <p>
 * 用于返回用户的志愿服务时长、申请次数等统计信息。
 * 作为独立DTO类，确保OpenAPI/Swagger可正确生成Schema文档。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "志愿服务统计")
public class VolunteerStatsDto {

    @Schema(description = "总服务记录数")
    private Long totalRecords;

    @Schema(description = "总服务时长（小时）")
    private BigDecimal totalHours;

    @Schema(description = "待审核记录数")
    private Long pendingCount;
}
