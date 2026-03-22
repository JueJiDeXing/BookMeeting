// MeetingRoomService.java
package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.dto.admin.room.RoomAddRequest;
import com.jjdx.bookmeeting.model.dto.admin.room.RoomQueryRequest;
import com.jjdx.bookmeeting.model.dto.admin.room.RoomUpdateRequest;
import com.jjdx.bookmeeting.model.dto.user.room.UserRoomQueryRequest;
import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import com.jjdx.bookmeeting.model.vo.RoomVO;
import com.jjdx.bookmeeting.service.params.RoomQueryParams;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * 会议室服务
 */
public interface MeetingRoomService extends IService<MeetingRoom> {

    /**
     * 添加会议室
     */
    MeetingRoom addRoom(RoomAddRequest roomAddRequest);

    /**
     * 更新会议室
     */
    boolean updateRoom(RoomUpdateRequest roomUpdateRequest);

    /**
     * 更新会议室设备
     */
    boolean updateRoomEquipment(Long roomId, List<Long> equipmentIds);

    /**
     * 检查是否有未完成的预定
     */
    boolean checkActiveBookings(Long roomId);

    // MeetingRoomService.java

    /**
     * 获取查询条件包装器（使用通用参数）
     */
    QueryWrapper<MeetingRoom> getQueryWrapper(RoomQueryParams params);

    /**
     * 管理员端获取查询条件包装器
     */
    default QueryWrapper<MeetingRoom> getQueryWrapper(RoomQueryRequest request) {
        RoomQueryParams params = new RoomQueryParams();
        BeanUtils.copyProperties(request, params);
        return getQueryWrapper(params);
    }

    /**
     * 用户端获取查询条件包装器
     */
    default QueryWrapper<MeetingRoom> getQueryWrapper(UserRoomQueryRequest request) {
        RoomQueryParams params = new RoomQueryParams();
        BeanUtils.copyProperties(request, params);
        // 用户端只查询可用会议室
        params.setStatus(0);
        params.setIsDelete(0);
        return getQueryWrapper(params);
    }
    /**
     * 获取会议室VO（带设备信息）
     */
    RoomVO getRoomVO(MeetingRoom room);

    /**
     * 生成位置描述
     */
    default String generateLocationDesc(String building, Integer floor, String roomNumber) {
        return building + " " + floor + "楼 " + roomNumber;
    }
}
