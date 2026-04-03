package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 会议室状态枚举
 */
@Getter
public enum RoomStatusEnum {

    AVAILABLE(0, "可用"),
    MAINTENANCE(1, "维护中"),
    USE(2, "不可用"),
    ;

    private final Integer value;
    private final String text;

    RoomStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(Integer value) {
        for (RoomStatusEnum RoomStatusEnum : RoomStatusEnum.values()) {
            if (RoomStatusEnum.getValue().equals(value)) {
                return RoomStatusEnum.getText();
            }
        }
        return "未知";
    }

    public static boolean isValidEnum(int value) {
        return value == RoomStatusEnum.AVAILABLE.value
                || value == RoomStatusEnum.MAINTENANCE.value
                || value == RoomStatusEnum.USE.value
                ;
    }

    public static boolean isAvailable(int value) {
        return value == RoomStatusEnum.AVAILABLE.value;
    }

    public static boolean isMaintenance(int value) {
        return value == RoomStatusEnum.MAINTENANCE.value;
    }

    public static boolean isUse(int value) {
        return value == RoomStatusEnum.USE.value;
    }
}
