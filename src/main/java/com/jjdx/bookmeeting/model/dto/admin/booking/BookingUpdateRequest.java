package com.jjdx.bookmeeting.model.dto.admin.booking;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 预定更新请求
 */
@Data
public class BookingUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     预定记录ID
     */
    private Long id;

    /**
     会议标题
     */
    private String title;

    /**
     会议室ID
     */
    private Long roomId;

    /**
     预定人ID
     */
    private Long userId;

    /**
     参会人员ID列表
     */
    private List<Long> attendeeIds;

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
     会议描述
     */
    private String description;

    /**
     会议状态
     */
    private Integer status;
}
