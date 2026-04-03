package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 提醒方式枚举
 */
@Getter
public enum RemindTypeEnum {

    SITE_MESSAGE(0, "站内信"),
    EMAIL(1, "邮件"),
    ALL(2, "全部");

    private final int value;
    private final String description;

    RemindTypeEnum(int value, String description) {
        this.value = value;
        this.description = description;
    }

}
