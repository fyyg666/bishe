package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * <p>
 * 记录系统中关键操作的审计日志，包括操作模块、操作类型、执行方法、
 * 请求参数、返回结果、操作用户信息、IP地址及执行时长等。
 * 用于系统安全审计和行为追踪。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作模块（如：认证、图书、借阅、座位等） */
    private String module;

    /** 操作类型（如：登录、新增、修改、删除、查询等） */
    private String operation;

    /** 目标方法全限定名 */
    private String method;

    /** 请求参数（JSON格式） */
    private String params;

    /** 返回结果（JSON格式，截取前2000字符） */
    private String result;

    /** 操作用户ID */
    private Long userId;

    /** 操作用户名 */
    private String username;

    /** 操作者IP地址 */
    private String ip;

    /** 操作地点（基于IP解析，可选） */
    private String location;

    /** 方法执行时长（毫秒） */
    private Long duration;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记：0-正常，1-已删除 */
    @TableLogic
    private Integer deleted;
}
