// RoomEquipmentServiceImpl.java
package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.RoomEquipmentMapper;
import com.jjdx.bookmeeting.model.entity.RoomEquipment;
import com.jjdx.bookmeeting.service.RoomEquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会议室设备关系服务实现
 */
@Service
@Slf4j
public class RoomEquipmentServiceImpl extends ServiceImpl<RoomEquipmentMapper, RoomEquipment> implements RoomEquipmentService {

    @Override
    public List<Long> getEquipmentIdsByRoomId(Long roomId) {
        LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomEquipment::getRoomId, roomId);
        
        return list(wrapper).stream()
                .map(RoomEquipment::getEquipmentId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoomIdsByEquipmentId(Long equipmentId) {
        LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomEquipment::getEquipmentId, equipmentId);
        
        return list(wrapper).stream()
                .map(RoomEquipment::getRoomId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRoomEquipments(Long roomId, List<Long> equipmentIds) {
        if (CollectionUtils.isEmpty(equipmentIds)) {
            return true;
        }

        List<RoomEquipment> roomEquipmentList = equipmentIds.stream()
                .map(equipmentId -> {
                    RoomEquipment roomEquipment = new RoomEquipment();
                    roomEquipment.setRoomId(roomId);
                    roomEquipment.setEquipmentId(equipmentId);
                    roomEquipment.setIsAvailable(1);
                    return roomEquipment;
                })
                .collect(Collectors.toList());

        return saveBatch(roomEquipmentList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRoomEquipments(Long roomId, List<Long> equipmentIds) {
        // 先删除原有关系
        removeByRoomId(roomId);
        
        // 再添加新关系
        return addRoomEquipments(roomId, equipmentIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByRoomId(Long roomId) {
        LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomEquipment::getRoomId, roomId);
        
        return remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByEquipmentId(Long equipmentId) {
        LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomEquipment::getEquipmentId, equipmentId);
        
        return remove(wrapper);
    }

    @Override
    public boolean checkRoomHasEquipment(Long roomId, Long equipmentId) {
        LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomEquipment::getRoomId, roomId)
                .eq(RoomEquipment::getEquipmentId, equipmentId);
        
        return count(wrapper) > 0;
    }

    @Override
    public int countEquipmentByRoomId(Long roomId) {
        LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomEquipment::getRoomId, roomId);
        
        return (int) count(wrapper);
    }
}
