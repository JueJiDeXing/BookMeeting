package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 参会响应状态枚举
 */
@Getter
public enum AttendeeResponseStatusEnum {

    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    REFUSE(2, "已拒绝"),
    ;

    private final Integer value;
    private final String text;

    AttendeeResponseStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(Integer value) {
        for (AttendeeResponseStatusEnum RoomStatusEnum : AttendeeResponseStatusEnum.values()) {
            if (RoomStatusEnum.getValue().equals(value)) {
                return RoomStatusEnum.getText();
            }
        }
        return "未知";
    }

    public static boolean isValidEnum(int value) {
        return value == AttendeeResponseStatusEnum.PENDING.value
                || value == AttendeeResponseStatusEnum.CONFIRMED.value
                || value == AttendeeResponseStatusEnum.REFUSE.value
                ;
    }

    public static boolean isPending(int value) {
        return value == AttendeeResponseStatusEnum.PENDING.value;
    }

    public static boolean isConfirmed(int value) {
        return value == AttendeeResponseStatusEnum.CONFIRMED.value;
    }

    public static boolean isRefuse(int value) {
        return value == AttendeeResponseStatusEnum.REFUSE.value;
    }
}
