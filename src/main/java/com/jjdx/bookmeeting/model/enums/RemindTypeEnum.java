package com.jjdx.bookmeeting.model.enums;

/**
 * 提醒方式枚举
 */
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

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
