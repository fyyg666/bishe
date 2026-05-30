package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.library.system.util.DataMaskingUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（加密存储） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号（JSON序列化时自动脱敏：138****5678） */
    @JsonSerialize(using = DataMaskingUtil.PhoneSerializer.class)
    private String phone;

    /** 邮箱（JSON序列化时自动脱敏：t***@example.com） */
    @JsonSerialize(using = DataMaskingUtil.EmailSerializer.class)
    private String email;

    /** 头像URL */
    private String avatar;

    /** 角色：ADMIN/LIBRARIAN/READER/VOLUNTEER */
    private String role;

    /** 状态：NORMAL-正常，DISABLED-禁用，LOCKED-锁定 */
    private String status;

    /** 积分 */
    private Integer creditScore;

    /** 读者卡号 */
    @TableField("card_number")
    private String cardNumber;

    /** 借阅数量 */
    @TableField("borrow_count")
    private Integer borrowCount;

    /** 违约次数 */
    private Integer violationCount;

    /** 封禁到期时间 */
    private LocalDateTime banUntil;

    /** 最大可借数量（非数据库字段，由业务逻辑计算） */ 
    @TableField(exist = false)
    private Integer maxBorrowCount;

    /**
     * 获取当前借阅数量
     */
    public Integer getCurrentBorrowCount() {
        return borrowCount;
    }

    /**
     * 获取最大可借数量（默认为5）
     */
    public Integer getMaxBorrowCount() {
        return maxBorrowCount != null ? maxBorrowCount : 5;
    }

    /** 版本号（乐观锁，用于并发控制） */
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
