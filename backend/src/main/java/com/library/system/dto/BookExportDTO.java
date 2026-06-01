package com.library.system.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookExportDTO {

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

    @ExcelProperty("总库存")
    private Integer totalCount;

    @ExcelProperty("可借数量")
    private Integer availableCount;

    @ExcelProperty("借阅次数")
    private Integer borrowCount;
}
