package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 志愿服务实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("volunteer_service")
public class VolunteerService implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    @TableField(exist = false)
    private String username;

    /** 用户真实姓名 */
    @TableField(exist = false)
    private String realName;

    /** 服务日期 */
    private LocalDateTime serviceDate;

    /** 服务开始时间 */
    private LocalDateTime startTime;

    /** 服务结束时间 */
    private LocalDateTime endTime;

    /** 服务时长（小时） */
    private BigDecimal serviceHours;

    /** 服务类型 */
    private String serviceType;

    /** 服务内容 */
    private String description;

    /** 状态：PENDING/APPROVED/REJECTED */
    private String status;

    /** 审核人ID */
    @TableField("reviewer_id")
    private Long reviewerId;

    /** 审核人姓名 */
    @TableField(exist = false)
    private String reviewerName;

    /** 审核时间 */
    @TableField("review_time")
    private LocalDateTime reviewTime;

    /** 审核备注 */
    @TableField("review_remark")
    private String reviewRemark;

    /** 版本号（乐观锁，数据库暂无此列） */
    @TableField(exist = false)
    private Integer version;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
