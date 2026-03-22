package com.jjdx.bookmeeting.model.dto.admin.equipment;

import com.jjdx.bookmeeting.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备日志查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EquipmentLogQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private Long equipmentId;

    /**
     * 设备名称（模糊查询）
     */
    private String equipmentName;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作类型（1-新增 2-移入 3-移出 4-维修 5-报废）
     */
    private Integer operationType;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
