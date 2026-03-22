package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会议提醒任务表实体类
 */
@Data
@TableName("remind_task")
public class RemindTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提醒任务ID
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
     * 提醒时间
     */
    private LocalDateTime remindTime;

    /**
     * 提醒方式（0-站内信 1-邮件 2-全部）
     */
    private Integer remindType;

    /**
     * 状态（0-待发送 1-已发送 2-发送失败 3-已取消）
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 错误信息
     */
    private String errorMsg;

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
