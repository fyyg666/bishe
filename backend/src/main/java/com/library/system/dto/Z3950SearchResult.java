package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Z3950SearchResult {

    private String sourceName;
    private int totalResults;
    private List<MarcRecordResponse> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarcRecordResponse {
        private String title;
        private String author;
        private String isbn;
        private String publisher;
        private String publishDate;
        private String rawMarc;
    }
}
