package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditRuleResponse {

    private String ruleKey;
    private String ruleName;
    private Integer score;
    private String description;
    private String type;
}
