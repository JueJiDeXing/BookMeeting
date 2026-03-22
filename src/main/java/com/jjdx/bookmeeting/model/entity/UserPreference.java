// UserPreference.java
package com.jjdx.bookmeeting.model.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户偏好（可以存到Redis或单独建表）
 */
@Data
public class UserPreference {
    
    private Long userId;
    
    /**
     * 常用的会议室ID列表（按使用频率排序）
     */
    private List<Long> favoriteRoomIds;
    
    /**
     * 常用的设备ID列表
     */
    private List<Long> preferredEquipmentIds;
    
    /**
     * 常用的时间段（格式：HH:mm-HH:mm）
     */
    private List<String> preferredTimeSlots;
    
    /**
     * 常用的楼栋
     */
    private String favoriteBuilding;
    
    /**
     * 平均参会人数
     */
    private Integer avgAttendeeCount;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
