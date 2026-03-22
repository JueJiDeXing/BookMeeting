package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备操作日志表实体类
 */
@Data
@TableName("equipment_log")
public class EquipmentLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID
     */
    private Long equipmentId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 原会议室ID（NULL表示仓库/未分配）
     */
    private Long fromRoomId;

    /**
     * 新会议室ID（NULL表示移出/报废）
     */
    private Long toRoomId;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
