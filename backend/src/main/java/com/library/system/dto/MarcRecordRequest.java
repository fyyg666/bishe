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
public class MarcRecordRequest {

    private String recordType;
    private String leader;
    private String controlNumber;
    private Long bookId;
    private List<FieldRequest> fields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldRequest {
        private String tag;
        private String indicator1;
        private String indicator2;
        private String subfields;
        private String displayValue;
        private Integer sortOrder;
    }
}
