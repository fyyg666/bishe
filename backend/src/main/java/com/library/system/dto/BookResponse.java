package com.library.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    /**
     * 图书ID
     */
    private Long id;

    /**
     * ISBN号
     */
    private String isbn;

    /**
     * 图书标题
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版日期
     */
    private String publishDate;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 图书描述
     */
    private String description;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 图书位置
     */
    private String location;

    /**
     * 总库存数量
     */
    private Integer totalCount;

    /**
     * 可借数量
     */
    private Integer availableCount;

    /**
     * 图书价格
     */
    private BigDecimal price;

    /**
     * 借阅次数
     */
    private Integer borrowCount;

    /**
     * 图书状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
