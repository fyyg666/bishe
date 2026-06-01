package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("z3950_source")
public class Z3950Source implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String host;

    private Integer port;

    private String database;

    private String protocol;

    private String charset;

    @TableField("is_enabled")
    private Boolean enabled;

    private Integer timeout;

    @TableLogic
    private Integer deleted;
}
