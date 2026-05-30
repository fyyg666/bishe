package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 座位实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("seat")
public class Seat implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 座位ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 阅览室ID */
    private Long roomId;

    /** 座位编号（如：A01） */
    private String seatNumber;

    /** 行号（数据库表无此字段，保留供后续扩展） */
    @TableField(exist = false)
    private Integer rowNum;

    /** 列号（数据库表无此字段，保留供后续扩展） */
    @TableField(exist = false)
    private Integer colNum;

    /** 状态：AVAILABLE/MAINTENANCE */
    private String status;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private String createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
