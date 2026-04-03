package com.jjdx.bookmeeting.service;

import com.jjdx.bookmeeting.model.dto.user.recommend.RecommendRequest;
import com.jjdx.bookmeeting.model.vo.recommend.RecommendRoomVO;

import java.util.List;

/**
 智能推荐服务接口
 */
public interface RecommendService {

    /**
     为用户推荐会议室
     */
    List<RecommendRoomVO> recommendRooms(Long userId, RecommendRequest request);

    /**
     获取热门会议室（基于使用频率）
     */
    List<RecommendRoomVO> getHotRooms(int limit);

    /**
     获取相似会议室（根据设备配置）
     */
    List<RecommendRoomVO> getSimilarRooms(Long roomId, int limit);
}
