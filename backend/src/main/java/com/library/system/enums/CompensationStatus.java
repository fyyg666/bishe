package com.library.system.enums;

/**
 * 赔偿状态枚举
 *
 * @author Library Team
 * @version 2.0.0
 */
public enum CompensationStatus {
    PENDING("待处理"),
    PAID("已赔付"),
    CANCELLED("已取消");

    private final String description;

    CompensationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
