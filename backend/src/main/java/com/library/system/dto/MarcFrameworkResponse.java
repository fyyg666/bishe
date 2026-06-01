package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarcFrameworkResponse {

    private Long id;
    private String name;
    private String code;
    private String recordType;
    private String description;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<FrameworkFieldDto> fields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrameworkFieldDto {
        private Long id;
        private String tag;
        private String indicator1;
        private String indicator2;
        private Integer required;
        private Integer repeatable;
        private String defaultSubfields;
        private Integer sortOrder;
    }
}
