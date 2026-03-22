package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 参会人员响应预定关系表实体类
 */
@Data
@TableName("attendee_response")
public class AttendeeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 预定记录ID
     */
    private Long bookingId;

    /**
     * 参会人ID
     */
    private Long userId;

    /**
     * 响应状态（0-待确认 1-已确认 2-已拒绝）
     */
    private Integer status;

    /**
     * 响应时间
     */
    private LocalDateTime responseTime;

    /**
     * 备注（如拒绝原因）
     */
    private String remark;

    /**
     * 是否从会议中删除（0-未删除 1-删除）
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
