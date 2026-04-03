package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.entity.RoomEquipment;

import java.util.List;

/**
 会议室设备关系服务
 */
public interface RoomEquipmentService extends IService<RoomEquipment> {

    /**
     根据会议室ID获取设备ID列表
     */
    List<Long> getEquipmentIdsByRoomId(Long roomId);

    /**
     根据设备ID获取会议室ID列表
     */
    List<Long> getRoomIdsByEquipmentId(Long equipmentId);

    /**
     批量添加会议室设备关系
     */
    boolean addRoomEquipments(Long roomId, List<Long> equipmentIds);

    /**
     更新会议室设备关系（先删除后添加）
     */
    boolean updateRoomEquipments(Long roomId, List<Long> equipmentIds);

    /**
     删除会议室的所有设备关系
     */
    boolean removeByRoomId(Long roomId);

    /**
     删除设备的所有会议室关系
     */
    boolean removeByEquipmentId(Long equipmentId);

    /**
     检查会议室是否包含某设备
     */
    boolean checkRoomHasEquipment(Long roomId, Long equipmentId);

    /**
     统计会议室设备数量
     */
    int countEquipmentByRoomId(Long roomId);
}
