package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 会议室含有设备关系表实体类
 */
@Data
@TableName("room_equipment")
public class RoomEquipment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     会议室ID
     */
    private Long roomId;

    /**
     设备ID
     */
    private Long equipmentId;

    /**
     设备是否可用（0-不可用 1-正常可用）
     */
    private Integer isAvailable;

    /**
     创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
