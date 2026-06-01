package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("book")
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 图书ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long branchId;

    /** ISBN编号 */
    private String isbn;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    @TableField(exist = false)
    private String categoryName;

    /** 出版社 */
    private String publisher;

    /** 出版日期 */
    private LocalDate publishDate;

    /** 价格 */
    private BigDecimal price;

    /** 总数量 */ 
    @TableField("total_quantity")
    private Integer totalCount;

    /** 可借数量 */ 
    @TableField("stock")
    private Integer availableCount;

    /** 馆藏位置（数据库暂无此列） */
    @TableField(exist = false)
    private String location;

    /** 借阅次数 */
    @TableField("borrow_count")
    private Integer borrowCount;

    /** 状态：0-上架（可借），1-下架（不可借）— 对应 Constants.BookStatus.NORMAL / OFFLINE */
    private Integer status;

    /** 描述 */
    private String description;

    /** 封面图片URL */
    @TableField("cover_image")
    private String coverImage;

    /** 版本号（乐观锁） */
    @Version
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
