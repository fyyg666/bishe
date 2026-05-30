package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 座位预约实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("seat_reservation")
public class SeatReservation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预约ID */
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

    /** 阅览室ID */
    private Long roomId;

    /** 阅览室名称 */
    @TableField(exist = false)
    private String roomName;

    /** 座位ID */
    private Long seatId;

    /** 座位编号 */
    @TableField(exist = false)
    private String seatNumber;

    /** 区域名称 */
    @TableField(exist = false)
    private String area;

    /** 预约日期（数据库为DATETIME类型，取日期部分） */ 
    private LocalDate reservationDate;

    /** 预约开始时间（数据库为DATETIME类型，取时间部分） */ 
    private LocalTime startTime;

    /** 预约结束时间（数据库为DATETIME类型，取时间部分） */ 
    private LocalTime endTime;

    /** 预约来源 */
    @TableField(exist = false)
    private String source;

    /** 取消原因 */
    @TableField(exist = false)
    private String cancelReason;

    /** 状态：PENDING/CHECKED_IN/COMPLETED/CANCELLED/NO_SHOW */
    private String status;

    /** 签到时间 */
    @TableField("check_in_time")
    private LocalDateTime checkInTime;

    /** 签退时间 */
    @TableField("check_out_time")
    private LocalDateTime checkOutTime;

    /** 违约次数（非数据库字段，通过User表维护） */
    @TableField(exist = false)
    private Integer violationCount;

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
