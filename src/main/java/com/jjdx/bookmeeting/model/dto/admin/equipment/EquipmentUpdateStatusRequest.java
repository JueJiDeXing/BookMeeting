package com.jjdx.bookmeeting.model.dto.admin.equipment;

import lombok.Data;

import java.io.Serializable;

/**
 设备更新状态请求
 */
@Data
public class EquipmentUpdateStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     设备ID
     */
    private Long id;

    /**
     状态（0-正常 1-不可用）
     */
    private Integer status;

    /**
     备注
     */
    private String remark;
}
