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
public class PurchaseSuggestionRequest {

    @NotBlank(message = "书名不能为空")
    private String title;

    private String author;

    private String isbn;

    private String reason;
}
