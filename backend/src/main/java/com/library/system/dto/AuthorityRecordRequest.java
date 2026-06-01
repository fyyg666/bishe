package com.library.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityRecordRequest {

    @NotBlank(message = "规范类型不能为空")
    private String authorityType;

    @NotBlank(message = "规范标目不能为空")
    private String heading;

    private String variantHeadings;

    private String source;

    private String sourceId;

    private String note;
}
