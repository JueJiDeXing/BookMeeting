package com.jjdx.bookmeeting.model.vo.admin.statistics;

import lombok.Data;

import java.util.List;

/**
 统计数据VO
 */
@Data
public class StatisticsVO {

    /**
     总览数据
     */
    private OverviewData overview;

    /**
     会议室使用排行
     */
    private List<RoomRankData> roomRank;

    /**
     时间段热度分布
     */
    private List<TimeHeatData> timeHeat;

    /**
     每日预定趋势
     */
    private List<TrendData> dailyTrend;

    /**
     预定人排行
     */
    private List<UserRankData> userRank;

    /**
     会议室使用详情
     */
    private List<RoomDetailData> roomDetails;
}

