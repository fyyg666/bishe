package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("announcement")
public class Announcement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公告ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型 */
    private String type;

    /** 优先级 */
    private Integer priority;

    /** 发布人ID */
    private Long publisherId;

    /** 发布人姓名 */
    @TableField(exist = false)
    private String publisherName;

    /** 状态：DRAFT/PUBLISHED/ARCHIVED */
    private String status;

    /** 发布时间 */
    private LocalDateTime publishTime;

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
