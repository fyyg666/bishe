package com.library.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {

    @NotBlank(message = "分馆名称不能为空")
    @Size(max = 100, message = "分馆名称最长100个字符")
    private String name;

    @NotBlank(message = "分馆编码不能为空")
    @Size(max = 50, message = "分馆编码最长50个字符")
    private String code;

    private String address;

    private String phone;

    private String email;

    private String openingHours;

    private Integer status;

    private Long parentId;
}
