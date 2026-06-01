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
public class BorrowExportDTO {

    @ExcelProperty("借阅ID")
    private Long id;

    @ExcelProperty("用户名")
    @ColumnWidth(12)
    private String username;

    @ExcelProperty("图书名称")
    @ColumnWidth(25)
    private String bookTitle;

    @ExcelProperty("ISBN")
    @ColumnWidth(18)
    private String bookIsbn;

    @ExcelProperty("借阅日期")
    @ColumnWidth(18)
    private String borrowDate;

    @ExcelProperty("到期日期")
    @ColumnWidth(18)
    private String dueDate;

    @ExcelProperty("归还日期")
    @ColumnWidth(18)
    private String returnDate;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("续借次数")
    private Integer renewCount;

    @ExcelProperty("逾期天数")
    private Integer overdueDays;

    @ExcelProperty("罚款金额")
    private BigDecimal fineAmount;
}
