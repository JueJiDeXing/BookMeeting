package com.jjdx.bookmeeting.model.vo;

import com.jjdx.bookmeeting.model.entity.Equipment;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 会议室视图对象（带设备信息）
 */
@Data
public class RoomVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     会议室ID
     */
    private Long id;

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
     完整位置描述
     */
    private String locationDesc;

    /**
     可容纳人数
     */
    private Integer capacity;

    /**
     会议室描述
     */
    private String description;

    /**
     状态（0-可用 1-维护中 2-被占用）
     */
    private Integer status;

    /**
     设备列表
     */
    private List<Equipment> equipmentList;

    /**
     创建时间
     */
    private LocalDateTime createTime;

    /**
     更新时间
     */
    private LocalDateTime updateTime;
}
