package com.jjdx.bookmeeting.model.enums;

/**
 * 预定状态枚举
 */
public enum BookingStatusEnum {

    PENDING(0, "待签到"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消"),
    TIMEOUT(4, "超时未签到");

    private final int value;
    private final String description;

    BookingStatusEnum(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static BookingStatusEnum getEnumByValue(int value) {
        for (BookingStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}
