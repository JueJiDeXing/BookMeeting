package com.jjdx.bookmeeting.model.enums;

import lombok.Getter;

/**
 * 设备操作类型枚举
 */
@Getter
public enum EquipmentOperationEnum {

    ADD(1, "新增"),
    MOVE_IN(2, "移入"),
    MOVE_OUT(3, "移出"),
    MAINTAIN(4, "维修"),
    SCRAP(5, "报废"),
    ;

    private final Integer value;
    private final String text;

    EquipmentOperationEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(Integer value) {
        for (EquipmentOperationEnum op : EquipmentOperationEnum.values()) {
            if (op.getValue().equals(value)) {
                return op.getText();
            }
        }
        return "未知";
    }
}
