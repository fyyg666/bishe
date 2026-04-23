package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 读者更新请求DTO
 * FIXED: QUAL-005 从ReaderController内部类提取为独立DTO文件
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderUpdateRequest {
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private String role;
    private String status;
    private Integer creditScore;
    private Integer maxBorrowCount;
}
