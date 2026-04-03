package com.jjdx.bookmeeting.model.dto.admin.statistics;

import lombok.Data;

import java.time.LocalDateTime;

/**
 统计查询请求
 */
@Data
public class StatisticsRequest {

    /**
     统计类型：day-日，week-周，month-月，quarter-季，year-年，custom-自定义
     */
    private String type;

    /**
     开始时间（自定义时使用）
     */
    private LocalDateTime startTime;

    /**
     结束时间（自定义时使用）
     */
    private LocalDateTime endTime;
}
