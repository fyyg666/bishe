package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅记录实体类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("borrow_record")
public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 借阅记录ID */
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

    /** 图书ID */
    private Long bookId;

    /** 图书名称 */
    @TableField(exist = false)
    private String bookTitle;

    /** ISBN */
    @TableField(exist = false)
    private String isbn;

    /** 图书ISBN（非数据库字段，关联查询填充） */ 
    @TableField(exist = false)
    private String bookIsbn;

    /** 借阅日期 */
    private LocalDateTime borrowDate;

    /** 应归还日期 */
    private LocalDateTime dueDate;

    /** 实际归还日期 */
    private LocalDateTime returnDate;

    /** 状态：BORROWING/RETURNED/OVERDUE/RESERVED */
    private String status;

    /** 续借次数 */
    private Integer renewCount;

    /** 逾期天数（非数据库字段，由业务计算） */ 
    @TableField(exist = false)
    private Integer overdueDays;

    /** 罚款金额 */
    private BigDecimal fineAmount;

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
