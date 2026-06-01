package com.library.system.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StatisticsExportDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewExport {
        @ExcelProperty("指标")
        @ColumnWidth(20)
        private String metric;
        @ExcelProperty("数值")
        private Long value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BorrowTrendExport {
        @ExcelProperty("日期")
        @ColumnWidth(15)
        private String date;
        @ExcelProperty("借阅数")
        private Long borrowCount;
        @ExcelProperty("归还数")
        private Long returnCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDistributionExport {
        @ExcelProperty("分类名称")
        @ColumnWidth(20)
        private String categoryName;
        @ExcelProperty("图书数量")
        private Long bookCount;
        @ExcelProperty("借阅次数")
        private Long borrowCount;
    }
}
