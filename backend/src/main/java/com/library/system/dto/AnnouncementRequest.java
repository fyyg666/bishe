package com.library.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公告请求DTO 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequest {

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    /**
     * 内容
     */
    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;

    /**
     * 类型：NOTICE/ACTIVITY/SYSTEM
     */
    @NotBlank(message = "公告类型不能为空")
    @Pattern(regexp = "^(NOTICE|ACTIVITY|SYSTEM)$", message = "公告类型必须为NOTICE/ACTIVITY/SYSTEM")
    private String type;

    /**
     * 优先级
     */
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    /**
     * 状态：DRAFT/PUBLISHED/ARCHIVED
     */
    @Pattern(regexp = "^(DRAFT|PUBLISHED|ARCHIVED)$", message = "状态必须为DRAFT/PUBLISHED/ARCHIVED")
    private String status;
}
