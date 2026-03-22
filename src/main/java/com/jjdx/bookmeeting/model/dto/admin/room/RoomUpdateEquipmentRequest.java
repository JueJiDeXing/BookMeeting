// RoomUpdateEquipmentRequest.java
package com.jjdx.bookmeeting.model.dto.admin.room;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 会议室更新设备请求
 */
@Data
public class RoomUpdateEquipmentRequest implements Serializable {

    /**
     * 会议室ID
     */
    private Long roomId;

    /**
     * 设备ID列表
     */
    private List<Long> equipmentIds;

    private static final long serialVersionUID = 1L;
}
