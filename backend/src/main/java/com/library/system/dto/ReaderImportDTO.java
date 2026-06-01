package com.library.system.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderImportDTO {

    @ExcelProperty("用户名")
    @ColumnWidth(15)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @ExcelProperty("姓名")
    @ColumnWidth(12)
    private String realName;

    @ExcelProperty("手机号")
    @ColumnWidth(15)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @ExcelProperty("邮箱")
    @ColumnWidth(22)
    private String email;

    @ExcelProperty("角色")
    @ColumnWidth(10)
    private String role;
}
