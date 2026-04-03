package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 预定状态枚举
 */
@Getter
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

    public static BookingStatusEnum getEnumByValue(int value) {
        for (BookingStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public static boolean isPending(int value) {
        return value == BookingStatusEnum.PENDING.value;
    }

    public static boolean isInProgress(int value) {
        return value == BookingStatusEnum.IN_PROGRESS.value;
    }

    public static boolean isCompleted(int value) {
        return value == BookingStatusEnum.COMPLETED.value;
    }

    public static boolean isCancelled(int value) {
        return value == BookingStatusEnum.CANCELLED.value;
    }

    public static boolean isTimeout(int value) {
        return value == BookingStatusEnum.TIMEOUT.value;
    }

}
