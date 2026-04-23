package com.library.system.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 图书请求DTO（新增/更新）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {

    /**
     * 图书ID（更新时必填）
     */
    private Long id;

    /**
     * ISBN号
     */
    @NotBlank(message = "ISBN不能为空")
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
            message = "ISBN格式不正确")
    private String isbn;

    /**
     * 图书标题
     */
    @NotBlank(message = "图书标题不能为空")
    @Size(max = 200, message = "图书标题长度不能超过200个字符")
    private String title;

    /**
     * 作者
     */
    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过100个字符")
    private String author;

    /**
     * 出版社
     */
    @NotBlank(message = "出版社不能为空")
    @Size(max = 100, message = "出版社长度不能超过100个字符")
    private String publisher;

    /**
     * 出版日期
     */
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "出版日期格式应为yyyy-MM")
    private String publishDate;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 图书描述
     */
    @Size(max = 2000, message = "图书描述长度不能超过2000个字符")
    private String description;

    /**
     * 封面图片URL
     */
    @Size(max = 500, message = "封面图片URL长度不能超过500个字符")
    private String coverImage;

    /**
     * 图书位置
     */
    @Size(max = 50, message = "图书位置长度不能超过50个字符")
    private String location;

    /**
     * 总库存数量
     */
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能小于0")
    @Max(value = 9999, message = "库存数量不能超过9999")
    private Integer totalCount;

    /**
     * 图书价格
     */
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    @DecimalMax(value = "99999.99", message = "价格不能超过99999.99")
    private BigDecimal price;

    /**
     * 图书状态：0-下架，1-可借，2-仅阅览
     */
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 2, message = "状态值不正确")
    private Integer status;
}
