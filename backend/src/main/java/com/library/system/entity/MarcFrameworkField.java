package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("marc_framework_field")
public class MarcFrameworkField implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long frameworkId;

    private String tag;

    private String indicator1;

    private String indicator2;

    private Integer required;

    private Integer repeatable;

    private String defaultSubfields;

    private Integer sortOrder;
}
