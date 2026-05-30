package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分日志实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("credit_log")
public class CreditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    @TableField(exist = false)
    private String username;

    /** 变动值（正数为增加，负数为减少） */
    @TableField("credit_change")
    private Integer creditChange;

    /** 变动后积分 */
    @TableField("credit_balance")
    private Integer creditBalance;

    /** 变动类型 */
    private String changeType;

    /** 相关业务ID（借阅记录ID或预约记录ID） */
    @TableField("biz_id")
    private Long bizId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    // 兼容方法
    public Integer getChangeValue() {
        return creditChange;
    }

    public Integer getBalance() {
        return creditBalance;
    }

    public String getType() {
        return changeType;
    }

    public String getDescription() {
        return remark;
    }
}
