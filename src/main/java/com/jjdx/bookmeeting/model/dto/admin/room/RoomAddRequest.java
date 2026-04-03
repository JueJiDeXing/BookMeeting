package com.jjdx.bookmeeting.model.dto.admin.room;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 会议室创建请求
 */
@Data
public class RoomAddRequest implements Serializable {

    /**
     会议室名称
     */
    private String roomName;

    /**
     楼栋
     */
    private String building;

    /**
     楼层
     */
    private Integer floor;

    /**
     房间号
     */
    private String roomNumber;

    /**
     可容纳人数
     */
    private Integer capacity;

    /**
     会议室描述
     */
    private String description;

    /**
     状态（0-可用 1-维护中）
     */
    private Integer status;

    /**
     设备ID列表
     */
    private List<Long> equipmentIds;

    private static final long serialVersionUID = 1L;
}
