package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityRecordResponse {

    private Long id;
    private String authorityType;
    private String heading;
    private String variantHeadings;
    private String source;
    private String sourceId;
    private String note;
    private LocalDateTime createTime;
}
