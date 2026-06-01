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
public class MarcRecordResponse {

    private Long id;
    private String recordType;
    private String leader;
    private String controlNumber;
    private Long bookId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MarcFieldDto> fields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarcFieldDto {
        private Long id;
        private String tag;
        private String indicator1;
        private String indicator2;
        private String subfields;
        private String displayValue;
        private Integer sortOrder;
    }
}
