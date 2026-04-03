package com.jjdx.bookmeeting.model.vo.admin.statistics;

import lombok.Data;

@Data
public class TrendData {
    /**
     日期
     */
    private String date;
    /**
     预定次数
     */
    private Integer count;
    /**
     总时长（小时）
     */
    private Double totalHours;
}
