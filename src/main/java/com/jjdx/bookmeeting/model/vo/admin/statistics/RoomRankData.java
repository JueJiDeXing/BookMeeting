package com.jjdx.bookmeeting.model.vo.admin.statistics;

import lombok.Data;

@Data
public class RoomRankData {
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
}
