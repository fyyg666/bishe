package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedSearchResponse {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String resourceType;
    private String format;
    private String coverUrl;
    private String accessUrl;
    private Integer availableCount;
}
