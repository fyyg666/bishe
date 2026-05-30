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

    /** 公告类型：NOTICE-通知，ACTIVITY-活动，URGENT-紧急 */
    private String type;

    /** 优先级：数字越大越靠前 */
    private Integer priority;

    /** 状态：DRAFT-草稿，PUBLISHED-已发布，ARCHIVED-已归档 */
    private String status;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 是否置顶：0-否，1-是 */
    @TableField("is_top")
    private Integer isTop;

    /** 浏览次数 */
    @TableField("view_count")
    private Integer viewCount;

    /** 发布人ID */
    @TableField("created_by")
    private Long publisherId;

    /** 发布人姓名 */
    @TableField(exist = false)
    private String publisherName;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
