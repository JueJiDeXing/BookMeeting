package com.jjdx.bookmeeting.model.vo.admin.statistics;

import lombok.Data;

// RoomDetailData.java
@Data
public class RoomDetailData {
    /**
     会议室ID
     */
    private Long roomId;
    /**
     会议室名称
     */
    private String roomName;
    /**
     预定次数
     */
    private Integer bookingCount;
    /**
     使用时长（小时）
     */
    private Double totalHours;
    /**
     使用率（%）
     */
    private Double utilizationRate;
    /**
     最常用时间段
     */
    private String peakTime;
}
