// RecommendServiceImpl.java
package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.jjdx.bookmeeting.model.dto.user.recommend.RecommendRequest;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import com.jjdx.bookmeeting.model.entity.UserPreference;
import com.jjdx.bookmeeting.model.vo.recommend.RecommendRoomVO;
import com.jjdx.bookmeeting.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐服务实现
 */
@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {
    
    @Resource
    private MeetingRoomService meetingRoomService;
    
    @Resource
    private RoomEquipmentService roomEquipmentService;
    
    @Resource
    private BookingRecordService bookingRecordService;
    
    @Resource
    private UserPreferenceService userPreferenceService;
    
    // 权重配置
    private static final double WEIGHT_CAPACITY = 0.35;
    private static final double WEIGHT_EQUIPMENT = 0.30;
    private static final double WEIGHT_HISTORY = 0.20;
    private static final double WEIGHT_TIME = 0.10;
    private static final double WEIGHT_HOT = 0.05;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    @Override
    public List<RecommendRoomVO> recommendRooms(Long userId, RecommendRequest request) {
        // 1. 获取用户偏好
        UserPreference preference = userPreferenceService.getUserPreference(userId);
        
        // 2. 获取所有可用会议室
        List<MeetingRoom> rooms = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getStatus, 0)
                .eq(MeetingRoom::getIsDelete, 0)
                .list();
        
        if (CollectionUtils.isEmpty(rooms)) {
            return new ArrayList<>();
        }
        
        // 3. 计算每个会议室的推荐分数
        List<RecommendRoomVO> scoredRooms = new ArrayList<>();
        
        for (MeetingRoom room : rooms) {
            RecommendRoomVO roomVO = calculateRoomScore(room, preference, request, userId);
            if (roomVO != null) {
                scoredRooms.add(roomVO);
            }
        }
        
        // 4. 按分数排序
        scoredRooms.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // 5. 返回Top N
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        return scoredRooms.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RecommendRoomVO> getHotRooms(int limit) {
        // 统计各会议室使用次数
        List<BookingRecord> bookings = bookingRecordService.lambdaQuery()
                .in(BookingRecord::getStatus, 0, 1, 2) // 有效预定
                .list();
        
        Map<Long, Long> usageCount = bookings.stream()
                .collect(Collectors.groupingBy(BookingRecord::getRoomId, Collectors.counting()));
        
        // 获取会议室信息并计算热度分数
        List<MeetingRoom> rooms = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getStatus, 0)
                .list();
        
        List<RecommendRoomVO> hotRooms = new ArrayList<>();
        for (MeetingRoom room : rooms) {
            Long count = usageCount.getOrDefault(room.getId(), 0L);
            RecommendRoomVO vo = convertToRecommendVO(room);
            // 热度分数 = 使用次数 / 最大使用次数 * 100
            double maxCount = usageCount.values().stream().max(Long::compare).orElse(1L);
            vo.setScore(maxCount > 0 ? count * 100.0 / maxCount : 0);
            vo.setMatchReason("热门会议室，已被预定" + count + "次");
            hotRooms.add(vo);
        }
        
        hotRooms.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return hotRooms.stream().limit(limit).collect(Collectors.toList());
    }
    
    @Override
    public List<RecommendRoomVO> getSimilarRooms(Long roomId, int limit) {
        // 获取源会议室的设备列表
        List<Long> sourceEquipmentIds = roomEquipmentService.getEquipmentIdsByRoomId(roomId);
        if (CollectionUtils.isEmpty(sourceEquipmentIds)) {
            return new ArrayList<>();
        }
        
        // 获取所有会议室
        List<MeetingRoom> rooms = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getStatus, 0)
                .ne(MeetingRoom::getId, roomId)
                .list();
        
        // 计算相似度
        List<RecommendRoomVO> similarRooms = new ArrayList<>();
        for (MeetingRoom room : rooms) {
            List<Long> targetEquipmentIds = roomEquipmentService.getEquipmentIdsByRoomId(room.getId());
            if (CollectionUtils.isEmpty(targetEquipmentIds)) {
                continue;
            }
            
            // 计算Jaccard相似度
            Set<Long> intersection = new HashSet<>(sourceEquipmentIds);
            intersection.retainAll(targetEquipmentIds);
            Set<Long> union = new HashSet<>(sourceEquipmentIds);
            union.addAll(targetEquipmentIds);
            double similarity = intersection.size() * 1.0 / union.size();
            
            RecommendRoomVO vo = convertToRecommendVO(room);
            vo.setScore(similarity * 100);
            vo.setMatchReason(String.format("设备配置相似度 %.0f%%", similarity * 100));
            similarRooms.add(vo);
        }
        
        similarRooms.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return similarRooms.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * 计算单个会议室的推荐分数
     */
    private RecommendRoomVO calculateRoomScore(MeetingRoom room, UserPreference preference, 
                                               RecommendRequest request, Long userId) {
        RecommendRoomVO vo = convertToRecommendVO(room);
        
        List<String> matchReasons = new ArrayList<>();
        double totalScore = 0;
        
        // 1. 容量匹配度（35%）
        if (request.getAttendeeCount() != null && request.getAttendeeCount() > 0) {
            double capacityScore = calculateCapacityMatch(room, request.getAttendeeCount());
            vo.setCapacityMatch(capacityScore);
            totalScore += capacityScore * WEIGHT_CAPACITY;
            if (capacityScore > 0.8) {
                matchReasons.add("容量合适");
            } else if (capacityScore < 0.3) {
                matchReasons.add("容量可能偏大或偏小");
            }
        } else {
            vo.setCapacityMatch(0.5);
            totalScore += 0.5 * WEIGHT_CAPACITY;
        }
        
        // 2. 设备匹配度（30%）
        if (CollectionUtils.isNotEmpty(request.getEquipmentIds())) {
            double equipmentScore = calculateEquipmentMatch(room.getId(), request.getEquipmentIds());
            vo.setEquipmentMatch(equipmentScore);
            totalScore += equipmentScore * WEIGHT_EQUIPMENT;
            if (equipmentScore == 1.0) {
                matchReasons.add("满足所有设备需求");
            } else if (equipmentScore > 0.5) {
                matchReasons.add("满足部分设备需求");
            }
        } else {
            vo.setEquipmentMatch(0.5);
            totalScore += 0.5 * WEIGHT_EQUIPMENT;
        }
        
        // 3. 历史偏好匹配度（20%）
        double historyScore = calculateHistoryMatch(room, preference);
        vo.setHistoryMatch(historyScore);
        totalScore += historyScore * WEIGHT_HISTORY;
        if (historyScore > 0.8) {
            matchReasons.add("根据历史推荐");
        }
        
        // 4. 时段匹配度（10%）
        double timeScore = calculateTimeMatch(room, request.getStartTime(), preference);
        vo.setTimeMatch(timeScore);
        totalScore += timeScore * WEIGHT_TIME;
        if (timeScore > 0.8) {
            matchReasons.add("您常在这个时间段开会");
        }
        
        // 5. 热度匹配（5%）
        double hotScore = calculateHotScore(room.getId());
        totalScore += hotScore * WEIGHT_HOT;
        
        // 6. 楼栋偏好加分（如果请求指定了楼栋）
        if (request.getBuilding() != null && request.getBuilding().equals(room.getBuilding())) {
            totalScore += 0.1;
            matchReasons.add("所在楼栋符合您的要求");
        }
        
        // 最终分数归一化到0-100
        double finalScore = Math.min(100, totalScore * 100);
        vo.setScore(finalScore);
        vo.setMatchReason(matchReasons.isEmpty() ? "综合推荐" : String.join(" · ", matchReasons));
        
        return vo;
    }
    
    /**
     * 计算容量匹配度
     */
    private double calculateCapacityMatch(MeetingRoom room, Integer attendeeCount) {
        int capacity = room.getCapacity();
        if (capacity < attendeeCount) {
            return 0; // 坐不下，直接淘汰
        }
        // 容量是人数1-2倍最合适
        double ratio = (double) capacity / attendeeCount;
        if (ratio <= 1.5) {
            return 1.0;
        } else if (ratio <= 2.0) {
            return 0.8;
        } else if (ratio <= 3.0) {
            return 0.5;
        } else {
            return 0.3;
        }
    }
    
    /**
     * 计算设备匹配度
     */
    private double calculateEquipmentMatch(Long roomId, List<Long> requiredEquipmentIds) {
        List<Long> roomEquipmentIds = roomEquipmentService.getEquipmentIdsByRoomId(roomId);
        if (CollectionUtils.isEmpty(roomEquipmentIds)) {
            return 0;
        }
        long matchCount = requiredEquipmentIds.stream()
                .filter(roomEquipmentIds::contains)
                .count();
        return (double) matchCount / requiredEquipmentIds.size();
    }
    
    /**
     * 计算历史偏好匹配度
     */
    private double calculateHistoryMatch(MeetingRoom room, UserPreference preference) {
        if (preference == null || CollectionUtils.isEmpty(preference.getFavoriteRoomIds())) {
            return 0.5;
        }
        // 用户常用的会议室加分
        if (preference.getFavoriteRoomIds().contains(room.getId())) {
            return 1.0;
        }
        // 用户常用的楼栋加分
        if (preference.getFavoriteBuilding() != null && 
            preference.getFavoriteBuilding().equals(room.getBuilding())) {
            return 0.7;
        }
        return 0.3;
    }
    
    /**
     * 计算时段匹配度
     */
    private double calculateTimeMatch(MeetingRoom room, LocalDateTime startTime, UserPreference preference) {
        if (startTime == null || preference == null || CollectionUtils.isEmpty(preference.getPreferredTimeSlots())) {
            return 0.5;
        }
        String targetSlot = String.format("%02d:00-%02d:00", startTime.getHour(), startTime.getHour() + 1);
        if (preference.getPreferredTimeSlots().contains(targetSlot)) {
            return 1.0;
        }
        return 0.3;
    }
    
    /**
     * 计算热度分数（基于预定次数）
     */
    private double calculateHotScore(Long roomId) {
        // 统计最近30天的预定次数
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long count = bookingRecordService.lambdaQuery()
                .eq(BookingRecord::getRoomId, roomId)
                .ge(BookingRecord::getCreateTime, thirtyDaysAgo)
                .count();
        
        // 假设最多30次，归一化
        return Math.min(1.0, count / 30.0);
    }
    
    /**
     * 转换为推荐VO
     */
    private RecommendRoomVO convertToRecommendVO(MeetingRoom room) {
        RecommendRoomVO vo = new RecommendRoomVO();
        vo.setId(room.getId());
        vo.setRoomName(room.getRoomName());
        vo.setBuilding(room.getBuilding());
        vo.setFloor(room.getFloor());
        vo.setRoomNumber(room.getRoomNumber());
        vo.setLocationDesc(room.getLocationDesc());
        vo.setCapacity(room.getCapacity());
        vo.setDescription(room.getDescription());
        vo.setImageUrl(room.getImageUrl());
        vo.setStatus(room.getStatus());
        vo.setCreateTime(room.getCreateTime());
        
        // 获取设备列表
        List<Long> equipmentIds = roomEquipmentService.getEquipmentIdsByRoomId(room.getId());
        if (CollectionUtils.isNotEmpty(equipmentIds)) {
            // 这里需要设备服务获取设备详情
        }
        
        return vo;
    }
}
