package com.jjdx.bookmeeting.service;

import com.jjdx.bookmeeting.model.entity.UserPreference;

import java.util.List;

/**
 用户偏好服务接口
 */
public interface UserPreferenceService {

    /**
     获取用户偏好
     */
    UserPreference getUserPreference(Long userId);

    /**
     更新用户偏好（基于历史预定）
     */
    void updateUserPreference(Long userId);

    /**
     获取用户常用的时间段（小时段，如 "09:00-10:00"）
     */
    List<String> getUserPreferredTimeSlots(Long userId);

    /**
     获取用户常用的设备ID列表
     */
    List<Long> getUserPreferredEquipmentIds(Long userId);

    /**
     获取用户常用的会议室ID列表（按使用次数排序）
     */
    List<Long> getUserFavoriteRoomIds(Long userId);
}
