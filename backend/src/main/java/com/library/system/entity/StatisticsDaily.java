package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日统计实体
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@TableName("statistics_daily")
public class StatisticsDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    private Integer totalBorrows;

    private Integer totalReturns;

    private Integer totalOverdue;

    private Integer totalReservations;

    private Integer totalCheckins;

    private Integer totalNewUsers;

    private LocalDateTime createTime;
}
