package com.jjdx.bookmeeting.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备日志视图对象
 */
@Data
public class EquipmentLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作日志ID
     */
    private Long id;

    /**
     * 设备ID
     */
    private Long equipmentId;

    /**
     * 设备名称
     */
    private String equipmentName;

    /**
     * 设备代码
     */
    private String equipmentCode;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 原会议室ID
     */
    private Long fromRoomId;

    /**
     * 原会议室名称
     */
    private String fromRoomName;

    /**
     * 新会议室ID
     */
    private Long toRoomId;

    /**
     * 新会议室名称
     */
    private String toRoomName;

    /**
     * 操作类型（1-新增 2-移入 3-移出 4-维修 5-报废）
     */
    private Integer operationType;

    /**
     * 原状态
     */
    private Integer oldStatus;

    /**
     * 新状态
     */
    private Integer newStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}
