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
public class ReportTemplateResponse {

    private Long id;

    private String name;

    private String description;

    private String sqlTemplate;

    private String parameters;

    private String category;

    private Long createdBy;

    private LocalDateTime createTime;
}
