package com.jjdx.bookmeeting.model.dto.user.booking;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户端检查时间可用性请求
 */
@Data
public class UserBookingCheckRequest implements Serializable {

    /**
     * 会议室ID
     */
    private Long roomId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}
