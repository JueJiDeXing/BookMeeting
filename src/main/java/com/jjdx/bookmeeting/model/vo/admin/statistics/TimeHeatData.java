package com.jjdx.bookmeeting.model.vo.admin.statistics;

import lombok.Data;

@Data
public class TimeHeatData {
    /**
     时间段（如 09:00-10:00）
     */
    private String timeSlot;
    /**
     预定次数
     */
    private Integer count;
}
