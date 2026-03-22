package com.jjdx.bookmeeting.model.dto.admin.equipment;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备移入/移出请求
 */
@Data
public class EquipmentMoveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private Long id;

    /**
     * 目标会议室ID（移入时必填，移出时可为空）
     */
    private Long targetRoomId;

    /**
     * 备注
     */
    private String remark;
}
