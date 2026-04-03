package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 提醒状态枚举
 */
@Getter
public enum RemindStatusEnum {

    PENDING(0, "待发送"),
    SENT(1, "已发送"),
    FAILED(2, "发送失败"),
    CANCELLED(3, "已取消");

    private final int value;
    private final String description;

    RemindStatusEnum(int value, String description) {
        this.value = value;
        this.description = description;
    }

}
