// RecommendRequest.java
package com.jjdx.bookmeeting.model.dto.user.recommend;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐请求参数
 */
@Data
public class RecommendRequest {
    
    /**
     * 参会人数（用于容量匹配）
     */
    private Integer attendeeCount;
    
    /**
     * 需要的设备ID列表
     */
    private List<Long> equipmentIds;
    
    /**
     * 期望的开始时间（可选，用于时间段热度匹配）
     */
    private LocalDateTime startTime;
    
    /**
     * 期望的结束时间（可选）
     */
    private LocalDateTime endTime;
    
    /**
     * 期望的楼栋（可选）
     */
    private String building;
    
    /**
     * 返回推荐数量，默认5
     */
    private Integer limit = 5;
}
