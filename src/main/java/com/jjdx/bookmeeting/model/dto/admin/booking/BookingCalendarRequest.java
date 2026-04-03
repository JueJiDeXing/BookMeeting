package com.jjdx.bookmeeting.model.dto.admin.booking;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 预定日历请求
 */
@Data
public class BookingCalendarRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     开始日期
     */
    private LocalDate startDate;

    /**
     结束日期
     */
    private LocalDate endDate;

    /**
     会议室ID（可选，查询特定会议室）
     */
    private Long roomId;

    /**
     用户ID（可选，查询特定用户）
     */
    private Long userId;
}
