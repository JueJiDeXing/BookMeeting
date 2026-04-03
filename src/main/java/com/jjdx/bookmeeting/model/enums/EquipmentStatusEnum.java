package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 设备状态枚举
 */
@Getter
public enum EquipmentStatusEnum {

    NORMAL(0, "正常"),
    UNAVAILABLE(1, "不可用"),
    ;

    private final Integer value;
    private final String text;

    EquipmentStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(Integer value) {
        for (EquipmentStatusEnum equipmentStatusEnum : EquipmentStatusEnum.values()) {
            if (equipmentStatusEnum.getValue().equals(value)) {
                return equipmentStatusEnum.getText();
            }
        }
        return "未知";
    }

    public static boolean isValidEnum(int value) {
        return value == EquipmentStatusEnum.NORMAL.value || value == EquipmentStatusEnum.UNAVAILABLE.value;
    }

    public static boolean isNormal(int value) {
        return value == EquipmentStatusEnum.NORMAL.value;
    }

    public static boolean isUnavailable(int value) {
        return value == EquipmentStatusEnum.UNAVAILABLE.value;
    }
}
