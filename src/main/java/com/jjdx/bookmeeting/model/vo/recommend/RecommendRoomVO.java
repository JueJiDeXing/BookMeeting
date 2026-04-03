package com.jjdx.bookmeeting.model.vo.recommend;

import com.jjdx.bookmeeting.model.vo.RoomVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 带推荐分数的会议室VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecommendRoomVO extends RoomVO {

    /**
     推荐分数（0-100）
     */
    private Double score;

    /**
     匹配详情（用于前端展示推荐理由）
     */
    private String matchReason;

    /**
     容量匹配度（0-1）
     */
    private Double capacityMatch;

    /**
     设备匹配度（0-1）
     */
    private Double equipmentMatch;

    /**
     历史偏好匹配度（0-1）
     */
    private Double historyMatch;

    /**
     时段匹配度（0-1）
     */
    private Double timeMatch;
}
