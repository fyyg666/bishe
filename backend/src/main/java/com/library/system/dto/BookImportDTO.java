package com.library.system.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookImportDTO {

    @ExcelProperty("书名")
    @ColumnWidth(25)
    private String title;

    @ExcelProperty("作者")
    @ColumnWidth(15)
    private String author;

    @ExcelProperty("ISBN")
    @ColumnWidth(18)
    private String isbn;

    @ExcelProperty("分类")
    @ColumnWidth(12)
    private String categoryName;

    @ExcelProperty("出版社")
    @ColumnWidth(18)
    private String publisher;

    @ExcelProperty("出版日期")
    @ColumnWidth(12)
    private String publishDate;

    @ExcelProperty("价格")
    private BigDecimal price;

    @ExcelProperty("总库存")
    private Integer totalCount;

    @ExcelProperty("简介")
    @ColumnWidth(30)
    private String description;
}
