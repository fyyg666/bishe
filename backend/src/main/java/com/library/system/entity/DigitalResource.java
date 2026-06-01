package com.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("digital_resource")
public class DigitalResource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long branchId;

    private String title;

    private String author;

    private String isbn;

    private String resourceType;

    private String format;

    private Long fileSize;

    private String accessUrl;

    private String provider;

    private String accessMode;

    private Long categoryId;

    private String description;

    private String coverUrl;

    private Integer status;

    private Integer borrowCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
