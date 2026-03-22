// EquipmentAddRequest.java
package com.jjdx.bookmeeting.model.dto.admin.equipment;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建设备请求
 */
@Data
public class EquipmentAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备名称
     */
    private String equipmentName;

    /**
     * 设备代码
     */
    private String equipmentCode;

    /**
     * 分类ID ✅ 新增
     */
    private Long categoryId;

    /**
     * 图标URL
     */
    private String icon;

    /**
     * 状态（0-正常 1-不可用）
     */
    private Integer status;
}
