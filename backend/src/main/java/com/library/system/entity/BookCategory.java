package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 图书分类实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("book_category")
public class BookCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类编码 */
    private String code;

    /** 父分类ID */
    private Long parentId;

    /** 排序顺序 */
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private String createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
