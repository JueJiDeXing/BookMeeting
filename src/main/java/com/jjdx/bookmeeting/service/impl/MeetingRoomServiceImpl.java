package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.constant.CommonConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.MeetingRoomMapper;
import com.jjdx.bookmeeting.model.dto.admin.room.RoomAddRequest;
import com.jjdx.bookmeeting.model.dto.admin.room.RoomUpdateRequest;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import com.jjdx.bookmeeting.model.entity.RoomEquipment;
import com.jjdx.bookmeeting.model.enums.RoomStatusEnum;
import com.jjdx.bookmeeting.model.vo.RoomVO;
import com.jjdx.bookmeeting.service.BookingRecordService;
import com.jjdx.bookmeeting.service.EquipmentService;
import com.jjdx.bookmeeting.service.MeetingRoomService;
import com.jjdx.bookmeeting.service.RoomEquipmentService;
import com.jjdx.bookmeeting.service.params.RoomQueryParams;
import com.jjdx.bookmeeting.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 会议室服务实现
 */
@Service
@Slf4j
public class MeetingRoomServiceImpl extends ServiceImpl<MeetingRoomMapper, MeetingRoom> implements MeetingRoomService {

    @Resource
    private RoomEquipmentService roomEquipmentService;

    @Resource
    private EquipmentService equipmentService;

    @Resource
    private BookingRecordService bookingRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingRoom addRoom(RoomAddRequest roomAddRequest) {
        // 1. 创建会议室
        MeetingRoom room = new MeetingRoom();
        BeanUtils.copyProperties(roomAddRequest, room);

        // 默认状态为可用
        if (room.getStatus() == null) {
            room.setStatus(RoomStatusEnum.AVAILABLE.getValue());
        }

        boolean saved = save(room);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加会议室失败");
        }

        // 2. 添加设备关联
        if (CollectionUtils.isNotEmpty(roomAddRequest.getEquipmentIds())) {
            List<RoomEquipment> roomEquipmentList = roomAddRequest.getEquipmentIds().stream()
                    .map(equipmentId -> {
                        RoomEquipment roomEquipment = new RoomEquipment();
                        roomEquipment.setRoomId(room.getId());
                        roomEquipment.setEquipmentId(equipmentId);
                        roomEquipment.setIsAvailable(1);
                        return roomEquipment;
                    })
                    .collect(Collectors.toList());
            roomEquipmentService.saveBatch(roomEquipmentList);
        }

        return room;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRoom(RoomUpdateRequest roomUpdateRequest) {
        // 1. 更新会议室基本信息
        MeetingRoom room = new MeetingRoom();
        BeanUtils.copyProperties(roomUpdateRequest, room);

        // 如果位置信息有更新，重新生成位置描述
        if (roomUpdateRequest.getBuilding() != null ||
                roomUpdateRequest.getFloor() != null ||
                roomUpdateRequest.getRoomNumber() != null) {

            MeetingRoom oldRoom = getById(roomUpdateRequest.getId());
            String building = roomUpdateRequest.getBuilding() != null ?
                    roomUpdateRequest.getBuilding() : oldRoom.getBuilding();
            Integer floor = roomUpdateRequest.getFloor() != null ?
                    roomUpdateRequest.getFloor() : oldRoom.getFloor();
            String roomNumber = roomUpdateRequest.getRoomNumber() != null ?
                    roomUpdateRequest.getRoomNumber() : oldRoom.getRoomNumber();

            room.setLocationDesc(generateLocationDesc(building, floor, roomNumber));
        }

        boolean updated = updateById(room);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新会议室失败");
        }

        // 2. 更新设备关联（如果传了设备列表）
        if (roomUpdateRequest.getEquipmentIds() != null) {
            // 删除原有设备关联
            roomEquipmentService.lambdaUpdate()
                    .eq(RoomEquipment::getRoomId, roomUpdateRequest.getId())
                    .remove();

            // 添加新的设备关联
            if (CollectionUtils.isNotEmpty(roomUpdateRequest.getEquipmentIds())) {
                List<RoomEquipment> roomEquipmentList = roomUpdateRequest.getEquipmentIds().stream()
                        .map(equipmentId -> {
                            RoomEquipment roomEquipment = new RoomEquipment();
                            roomEquipment.setRoomId(roomUpdateRequest.getId());
                            roomEquipment.setEquipmentId(equipmentId);
                            roomEquipment.setIsAvailable(1);
                            return roomEquipment;
                        })
                        .collect(Collectors.toList());
                roomEquipmentService.saveBatch(roomEquipmentList);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRoomEquipment(Long roomId, List<Long> equipmentIds) {
        // 删除原有设备关联
        roomEquipmentService.lambdaUpdate()
                .eq(RoomEquipment::getRoomId, roomId)
                .remove();

        // 添加新的设备关联
        if (CollectionUtils.isNotEmpty(equipmentIds)) {
            List<RoomEquipment> roomEquipmentList = equipmentIds.stream()
                    .map(equipmentId -> {
                        RoomEquipment roomEquipment = new RoomEquipment();
                        roomEquipment.setRoomId(roomId);
                        roomEquipment.setEquipmentId(equipmentId);
                        roomEquipment.setIsAvailable(1);
                        return roomEquipment;
                    })
                    .collect(Collectors.toList());
            return roomEquipmentService.saveBatch(roomEquipmentList);
        }

        return true;
    }

    @Override
    public boolean checkActiveBookings(Long roomId) {
        LocalDateTime now = LocalDateTime.now();

        // 查询该会议室是否有进行中的预定（状态为待签到或进行中）
        Long count = bookingRecordService.lambdaQuery()
                .eq(BookingRecord::getRoomId, roomId)
                .in(BookingRecord::getStatus, 0, 1) // 0-待签到 1-进行中
                .gt(BookingRecord::getEndTime, now)  // 结束时间大于当前时间
                .count();

        return count > 0;
    }

    @Override
    public QueryWrapper<MeetingRoom> getQueryWrapper(RoomQueryParams params) {
        if (params == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = params.getId();
        String roomName = params.getRoomName();
        String building = params.getBuilding();
        Integer floor = params.getFloor();
        Integer minCapacity = params.getMinCapacity();
        Integer maxCapacity = params.getMaxCapacity();
        Integer status = params.getStatus();
        Integer isDelete = params.getIsDelete();
        List<Long> categoryIds = params.getCategoryIds();


        String sortField = params.getSortField();
        String sortOrder = params.getSortOrder();

        QueryWrapper<MeetingRoom> queryWrapper = new QueryWrapper<>();

        // 精确查询
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(building), "building", building);
        queryWrapper.eq(floor != null, "floor", floor);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.eq(isDelete != null, "is_delete", isDelete);

        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(roomName), "room_name", roomName);

        // 容量范围查询
        queryWrapper.ge(minCapacity != null, "capacity", minCapacity);
        queryWrapper.le(maxCapacity != null, "capacity", maxCapacity);

        // 用户端：按设备分类筛选（必须同时包含所有指定分类的设备）
        if (CollectionUtils.isNotEmpty(categoryIds)) {
            // 子查询：找出包含所有指定分类设备的会议室ID
            // SELECT re.room_id
            // FROM room_equipment re
            // INNER JOIN equipment e ON re.equipment_id = e.id
            // WHERE e.category_id IN (1,2,3)
            // GROUP BY re.room_id
            // HAVING COUNT(DISTINCT e.category_id) = 3

            QueryWrapper<RoomEquipment> subQuery = new QueryWrapper<>();
            subQuery.inSql("equipment_id",
                            "SELECT id FROM equipment WHERE category_id IN (" +
                                    StringUtils.join(categoryIds, ",") + ")")
                    .groupBy("room_id")
                    .having("COUNT(DISTINCT (SELECT category_id FROM equipment WHERE id = equipment_id)) = {0}",
                            categoryIds.size());

            List<Object> roomIds = roomEquipmentService.listObjs(subQuery);

            if (CollectionUtils.isNotEmpty(roomIds)) {
                queryWrapper.in("id", roomIds);
            } else {
                // 没有找到符合条件的会议室
                queryWrapper.eq("id", 0);
            }
        }

        // 排序
        if (SqlUtils.validSortField(sortField)) {
            queryWrapper.orderBy(true, CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        } else {
            queryWrapper.orderByDesc("create_time");
        }

        return queryWrapper;
    }


    @Override
    public RoomVO getRoomVO(MeetingRoom room) {
        if (room == null) {
            return null;
        }

        RoomVO roomVO = new RoomVO();
        BeanUtils.copyProperties(room, roomVO);

        // 获取会议室设备列表
        List<RoomEquipment> roomEquipmentList = roomEquipmentService.lambdaQuery()
                .eq(RoomEquipment::getRoomId, room.getId())
                .list();

        if (CollectionUtils.isNotEmpty(roomEquipmentList)) {
            List<Long> equipmentIds = roomEquipmentList.stream()
                    .map(RoomEquipment::getEquipmentId)
                    .collect(Collectors.toList());

            List<Equipment> equipmentList = equipmentService.listByIds(equipmentIds);
            roomVO.setEquipmentList(equipmentList);
        }

        return roomVO;
    }
}
