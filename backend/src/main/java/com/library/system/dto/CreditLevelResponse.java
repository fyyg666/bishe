package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信用等级响应DTO
 * <p>
 * 包含当前积分、等级名称和下一等级所需积分。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditLevelResponse {

    /** 当前信用积分 */
    private Integer score;

    /** 等级名称：普通/铜牌/银牌/金牌/白金 */
    private String level;

    /** 下一等级所需积分（白金等级为null） */
    private Integer nextLevelScore;
}
