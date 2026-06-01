package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IsbnLookupResponse {

    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String publishDate;
    private String description;
    private String coverUrl;
    private Integer pageCount;
    private String source;
}
