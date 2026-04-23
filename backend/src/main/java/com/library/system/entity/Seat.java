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

    /** 座位编号 */
    private String seatNumber;

    /** 位置描述（如：A区-01） */
    private String location;

    /** 状态：AVAILABLE/OCCUPIED/RESERVED/MAINTENANCE */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private String createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
