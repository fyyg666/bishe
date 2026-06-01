package com.library.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String avatar;

    @Pattern(regexp = "^(READER|LIBRARIAN|ADMIN)$", message = "角色值不合法")
    private String role;

    @Pattern(regexp = "^(ACTIVE|DISABLED|BANNED)$", message = "状态值不合法")
    private String status;

    @Min(value = 0, message = "信用积分不能为负数")
    @Max(value = 200, message = "信用积分不能超过200")
    private Integer creditScore;

    @Min(value = 1, message = "最大借阅数至少为1")
    @Max(value = 50, message = "最大借阅数不能超过50")
    private Integer maxBorrowCount;
}
