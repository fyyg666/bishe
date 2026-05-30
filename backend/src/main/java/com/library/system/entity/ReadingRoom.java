package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 阅览室实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("reading_room")
public class ReadingRoom implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 阅览室ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 阅览室名称 */
    private String name;

    /** 阅览室编码（数据库暂无此列） */
    @TableField(exist = false)
    private String code;

    /** 位置描述 */
    private String location;

    /** 楼层（数据库暂无此列） */
    @TableField(exist = false)
    private String floor;

    /** 座位容量（映射SQL中的total_seats字段） */
    @TableField("total_seats")
    private Integer capacity;

    /** 开放时间 */
    private String openTime;

    /** 关闭时间 */
    private String closeTime;

    /** 状态：OPEN/CLOSED/MAINTENANCE */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private String createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
