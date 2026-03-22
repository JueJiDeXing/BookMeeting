package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 * 通用状态枚举
 */
@Getter
public enum StatusEnum {

    NORMAL(0, "正常"),
    UNAVAILABLE(1, "不可用"),
    ;

    private final Integer value;
    private final String text;

    StatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(Integer value) {
        for (StatusEnum statusEnum : StatusEnum.values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum.getText();
            }
        }
        return "未知";
    }
}
