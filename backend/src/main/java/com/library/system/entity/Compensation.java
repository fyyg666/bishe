package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 赔偿订单实体
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@TableName("compensation_order")
public class Compensation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long borrowId;

    private Long bookId;

    private String bookTitle;

    private String isbn;

    private String compType;

    private BigDecimal amount;

    private String status;

    private String paymentMethod;

    private Integer creditDeducted;

    private BigDecimal volunteerHours;

    private String remark;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
