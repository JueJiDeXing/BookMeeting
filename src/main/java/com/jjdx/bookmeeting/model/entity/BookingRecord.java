package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 用户预定会议室关系表实体类
 */
@Data
@TableName("booking_record")
public class BookingRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     预定记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     预定人ID
     */
    private Long userId;

    /**
     参会人员ID，含预定人，逗号分隔
     */
    private String attendeesId;

    /**
     会议室ID
     */
    private Long roomId;

    /**
     会议标题
     */
    private String title;

    /**
     会议描述
     */
    private String description;

    /**
     会议开始时间
     */
    private LocalDateTime startTime;

    /**
     会议结束时间
     */
    private LocalDateTime endTime;

    /**
     提前提醒分钟数
     */
    private Integer remindBefore;

    /**
     会议状态（0-待签到 1-进行中 2-已完成 3-已取消 4-未签到超时）
     */
    private Integer status;

    /**
     实际开始时间（签到时间）
     */
    private LocalDateTime actualStart;

    /**
     实际结束时间（释放时间）
     */
    private LocalDateTime actualEnd;

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
