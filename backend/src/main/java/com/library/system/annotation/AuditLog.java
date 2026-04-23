package com.library.system.annotation;

import java.lang.annotation.*;

/**
 * 操作日志审计注解
 * <p>
 * 标注在Controller方法上，用于声明需要记录操作日志的接口。
 * 配合 {@link com.library.system.aspect.OperationLogAspect} 切面使用，
 * 自动记录方法调用的模块、操作类型、请求参数、返回结果和执行时长等信息。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;AuditLog(module = "图书管理", operation = "新增图书")
 * &#64;PostMapping
 * public ApiResponse&lt;BookResponse&gt; createBook(...) { ... }
 * </pre>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作模块名称
     * <p>
     * 标识操作所属的功能模块，如"认证管理"、"图书管理"、"借阅管理"等。
     * </p>
     *
     * @return 模块名称
     */
    String module() default "";

    /**
     * 操作类型描述
     * <p>
     * 描述具体的操作行为，如"用户登录"、"新增图书"、"删除读者"等。
     * </p>
     *
     * @return 操作类型描述
     */
    String operation() default "";
}
