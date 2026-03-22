// UserPreferenceServiceImpl.java
package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.entity.UserPreference;
import com.jjdx.bookmeeting.service.BookingRecordService;
import com.jjdx.bookmeeting.service.RoomEquipmentService;
import com.jjdx.bookmeeting.service.UserPreferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户偏好服务实现
 */
@Service
@Slf4j
public class UserPreferenceServiceImpl implements UserPreferenceService {
    
    @Resource
    private BookingRecordService bookingRecordService;
    
    @Resource
    private RoomEquipmentService roomEquipmentService;
    
    // 简单的内存缓存（生产环境建议用Redis）
    private final Map<Long, UserPreference> preferenceCache = new HashMap<>();
    
    private static final DateTimeFormatter TIME_SLOT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    @Override
    public UserPreference getUserPreference(Long userId) {
        // 先从缓存获取
        if (preferenceCache.containsKey(userId)) {
            UserPreference cached = preferenceCache.get(userId);
            // 缓存超过1小时则刷新
            if (cached.getUpdateTime().isAfter(LocalDateTime.now().minusHours(1))) {
                return cached;
            }
        }
        
        // 重新计算
        updateUserPreference(userId);
        return preferenceCache.get(userId);
    }
    
    @Override
    public void updateUserPreference(Long userId) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setUpdateTime(LocalDateTime.now());
        
        // 1. 获取用户历史预定记录
        List<BookingRecord> history = bookingRecordService.lambdaQuery()
                .eq(BookingRecord::getUserId, userId)
                .orderByDesc(BookingRecord::getCreateTime)
                .list();
        
        if (CollectionUtils.isEmpty(history)) {
            // 无历史记录，返回默认偏好
            preference.setFavoriteRoomIds(new ArrayList<>());
            preference.setPreferredEquipmentIds(new ArrayList<>());
            preference.setPreferredTimeSlots(new ArrayList<>());
            preference.setAvgAttendeeCount(0);
            preferenceCache.put(userId, preference);
            return;
        }
        
        // 2. 统计常用会议室（按使用次数排序）
        Map<Long, Long> roomCountMap = history.stream()
                .collect(Collectors.groupingBy(BookingRecord::getRoomId, Collectors.counting()));
        List<Long> favoriteRoomIds = roomCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(5)
                .collect(Collectors.toList());
        preference.setFavoriteRoomIds(favoriteRoomIds);
        
        // 3. 统计常用设备（通过预定记录关联的会议室设备）
        Set<Long> equipmentSet = new HashSet<>();
        for (BookingRecord booking : history) {
            List<Long> equipmentIds = roomEquipmentService.getEquipmentIdsByRoomId(booking.getRoomId());
            equipmentSet.addAll(equipmentIds);
        }
        preference.setPreferredEquipmentIds(new ArrayList<>(equipmentSet));
        
        // 4. 统计常用时间段
        List<String> timeSlots = history.stream()
                .map(booking -> formatTimeSlot(booking.getStartTime()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(slot -> slot, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(3)
                .collect(Collectors.toList());
        preference.setPreferredTimeSlots(timeSlots);
        
        // 5. 统计常用楼栋
        // 需要从MeetingRoom获取，这里简化处理
        // 实际可以通过联查获取
        
        // 6. 计算平均参会人数
        double avgAttendee = history.stream()
                .mapToInt(booking -> {
                    String attendeesId = booking.getAttendeesId();
                    if (attendeesId == null) return 1;
                    return attendeesId.split(",").length;
                })
                .average()
                .orElse(0);
        preference.setAvgAttendeeCount((int) Math.round(avgAttendee));
        
        preferenceCache.put(userId, preference);
        log.info("更新用户[{}]偏好完成，常用会议室：{}，常用时间段：{}", 
                userId, favoriteRoomIds, timeSlots);
    }
    
    @Override
    public List<String> getUserPreferredTimeSlots(Long userId) {
        UserPreference pref = getUserPreference(userId);
        return pref.getPreferredTimeSlots();
    }
    
    @Override
    public List<Long> getUserPreferredEquipmentIds(Long userId) {
        UserPreference pref = getUserPreference(userId);
        return pref.getPreferredEquipmentIds();
    }
    
    @Override
    public List<Long> getUserFavoriteRoomIds(Long userId) {
        UserPreference pref = getUserPreference(userId);
        return pref.getFavoriteRoomIds();
    }
    
    /**
     * 格式化时间段（如 14:00-14:30 转为 14:00-15:00 的时段）
     */
    private String formatTimeSlot(LocalDateTime time) {
        if (time == null) return null;
        int hour = time.getHour();
        // 按小时时段分组：09:00-10:00, 10:00-11:00, ...
        return String.format("%02d:00-%02d:00", hour, hour + 1);
    }
}
