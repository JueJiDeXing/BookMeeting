package com.jjdx.bookmeeting.model.vo.admin.statistics;

import lombok.Data;

@Data
public class OverviewData {
    /**
     总预定次数
     */
    private Integer totalBookings;
    /**
     预定趋势（较上期变化百分比）
     */
    private Double bookingTrend;
    /**
     总会议时长（小时）
     */
    private Double totalHours;
    /**
     平均每场会议时长（小时）
     */
    private Double avgHoursPerBooking;
    /**
     参会总人次
     */
    private Integer totalAttendees;
    /**
     平均每场会议参会人数
     */
    private Double avgAttendeesPerBooking;
    /**
     会议室使用率（%）
     */
    private Double utilizationRate;
    /**
     高峰期时段
     */
    private String peakHours;
}
