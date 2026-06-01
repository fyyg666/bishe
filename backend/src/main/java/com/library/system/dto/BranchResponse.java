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
public class BranchResponse {

    private Long id;

    private String name;

    private String code;

    private String address;

    private String phone;

    private String email;

    private String openingHours;

    private Integer status;

    private Long parentId;

    private String parentName;

    private Integer childrenCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<BranchResponse> children;
}
