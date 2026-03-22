package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookingId;

    private String title;

    private String content;

    private Integer type;  // 0-会议提醒 1-系统通知

    private Integer status;  // 0-未读 1-已读

    private Long remindTaskId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime readTime;
}
