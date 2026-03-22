package com.jjdx.bookmeeting.model.dto.user.booking;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户端创建预定请求
 */
@Data
public class UserBookingAddRequest implements Serializable {

    /**
     * 会议室ID
     */
    private Long roomId;

    /**
     * 预定人ID（后端自动填充）
     */
    private Long userId;

    /**
     * 会议标题
     */
    private String title;

    /**
     * 会议开始时间
     */
    private LocalDateTime startTime;

    /**
     * 会议结束时间
     */
    private LocalDateTime endTime;

    /**
     * 参会人员ID列表
     */
    private List<Long> attendeeIds;

    /**
     * 提前提醒分钟数
     */
    private Integer remindBefore;

    /**
     * 会议描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
