package com.jjdx.bookmeeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.DeleteRequest;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.constant.UserConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.interceptor.aop.annotation.AuthCheck;
import com.jjdx.bookmeeting.model.dto.admin.room.*;
import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import com.jjdx.bookmeeting.model.enums.RoomStatusEnum;
import com.jjdx.bookmeeting.model.vo.RoomVO;
import com.jjdx.bookmeeting.service.MeetingRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 管理员-会议室管理接口
 */
@RestController
@RequestMapping("/admin/room")
@Slf4j
public class AdminRoomController {

    @Resource
    private MeetingRoomService meetingRoomService;

    // region 增删改查

    /**
     创建会议室
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addRoom(@RequestBody RoomAddRequest roomAddRequest, HttpServletRequest request) {
        if (roomAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验参数
        validateRoomAddRequest(roomAddRequest);

        // 检查会议室名称是否已存在
        MeetingRoom existingRoom = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getRoomName, roomAddRequest.getRoomName())
                .one();
        if (existingRoom != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议室名称已存在");
        }

        MeetingRoom room = meetingRoomService.addRoom(roomAddRequest);
        return ResultUtils.success(room.getId());
    }

    /**
     删除会议室
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteRoom(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查会议室是否存在
        MeetingRoom room = meetingRoomService.getById(deleteRequest.getId());
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议室不存在");
        }

        // 检查是否有未完成的预定
        boolean hasActiveBookings = meetingRoomService.checkActiveBookings(deleteRequest.getId());
        if (hasActiveBookings) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该会议室有未完成的预定，无法删除");
        }

        boolean removed = meetingRoomService.removeById(deleteRequest.getId());
        return ResultUtils.success(removed);
    }

    /**
     批量删除会议室
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteRoom(@RequestBody RoomBatchDeleteRequest batchDeleteRequest,
                                                 HttpServletRequest request) {
        if (batchDeleteRequest == null || batchDeleteRequest.getIds() == null || batchDeleteRequest.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查是否有未完成的预定
        for (Long id : batchDeleteRequest.getIds()) {
            boolean hasActiveBookings = meetingRoomService.checkActiveBookings(id);
            if (hasActiveBookings) {
                MeetingRoom room = meetingRoomService.getById(id);
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "会议室【" + room.getRoomName() + "】有未完成的预定，无法删除");
            }
        }

        boolean removed = meetingRoomService.removeByIds(batchDeleteRequest.getIds());
        return ResultUtils.success(removed);
    }

    /**
     更新会议室
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateRoom(@RequestBody RoomUpdateRequest roomUpdateRequest,
                                            HttpServletRequest request) {
        if (roomUpdateRequest == null || roomUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查会议室是否存在
        MeetingRoom oldRoom = meetingRoomService.getById(roomUpdateRequest.getId());
        if (oldRoom == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议室不存在");
        }

        // 校验参数
        validateRoomUpdateRequest(roomUpdateRequest);

        // 检查会议室名称是否与其他会议室重复
        if (roomUpdateRequest.getRoomName() != null && !roomUpdateRequest.getRoomName().equals(oldRoom.getRoomName())) {
            MeetingRoom existingRoom = meetingRoomService.lambdaQuery()
                    .eq(MeetingRoom::getRoomName, roomUpdateRequest.getRoomName())
                    .ne(MeetingRoom::getId, roomUpdateRequest.getId())
                    .one();
            if (existingRoom != null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议室名称已存在");
            }
        }

        boolean updated = meetingRoomService.updateRoom(roomUpdateRequest);
        return ResultUtils.success(updated);
    }

    /**
     更新会议室状态
     */
    @PostMapping("/update/status")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateRoomStatus(@RequestBody RoomUpdateStatusRequest statusRequest,
                                                  HttpServletRequest request) {
        if (statusRequest == null || statusRequest.getId() == null || statusRequest.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查会议室是否存在
        MeetingRoom room = meetingRoomService.getById(statusRequest.getId());
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议室不存在");
        }

        // 校验状态值
        if (!RoomStatusEnum.isValidEnum(statusRequest.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效");
        }

        // 如果要将状态改为"可用"，需要检查是否有进行中的预定
        if (RoomStatusEnum.isAvailable(statusRequest.getStatus())) {
            boolean hasActiveBookings = meetingRoomService.checkActiveBookings(statusRequest.getId());
            if (hasActiveBookings) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议室有进行中的预定，无法设为可用");
            }
        }

        room.setStatus(statusRequest.getStatus());
        boolean updated = meetingRoomService.updateById(room);
        return ResultUtils.success(updated);
    }

    /**
     更新会议室设备配置
     */
    @PostMapping("/update/equipment")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateRoomEquipment(@RequestBody RoomUpdateEquipmentRequest equipmentRequest,
                                                     HttpServletRequest request) {
        if (equipmentRequest == null || equipmentRequest.getRoomId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查会议室是否存在
        MeetingRoom room = meetingRoomService.getById(equipmentRequest.getRoomId());
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议室不存在");
        }

        boolean updated = meetingRoomService.updateRoomEquipment(equipmentRequest.getRoomId(),
                equipmentRequest.getEquipmentIds());
        return ResultUtils.success(updated);
    }

    /**
     根据 id 获取会议室
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<MeetingRoom> getRoomById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        MeetingRoom room = meetingRoomService.getById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(room);
    }

    /**
     根据 id 获取会议室（带设备信息）
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<RoomVO> getRoomVOById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        MeetingRoom room = meetingRoomService.getById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        RoomVO roomVO = meetingRoomService.getRoomVO(room);
        return ResultUtils.success(roomVO);
    }

    /**
     分页获取会议室列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<MeetingRoom>> listRoomByPage(@RequestBody RoomQueryRequest roomQueryRequest,
                                                          HttpServletRequest request) {
        if (roomQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = roomQueryRequest.getCurrent();
        long size = roomQueryRequest.getPageSize();

        Page<MeetingRoom> roomPage = meetingRoomService.page(
                new Page<>(current, size),
                meetingRoomService.getQueryWrapper(roomQueryRequest)
        );

        return ResultUtils.success(roomPage);
    }

    /**
     分页获取会议室列表（带设备信息）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<RoomVO>> listRoomVOByPage(@RequestBody RoomQueryRequest roomQueryRequest,
                                                       HttpServletRequest request) {
        if (roomQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = roomQueryRequest.getCurrent();
        long size = roomQueryRequest.getPageSize();

        Page<MeetingRoom> roomPage = meetingRoomService.page(
                new Page<>(current, size),
                meetingRoomService.getQueryWrapper(roomQueryRequest)
        );

        Page<RoomVO> roomVOPage = new Page<>(current, size, roomPage.getTotal());
        List<RoomVO> roomVOList = roomPage.getRecords().stream()
                .map(room -> meetingRoomService.getRoomVO(room))
                .collect(Collectors.toList());
        roomVOPage.setRecords(roomVOList);

        return ResultUtils.success(roomVOPage);
    }

    /**
     获取所有可用会议室列表
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<MeetingRoom>> listAllAvailableRooms(HttpServletRequest request) {
        List<MeetingRoom> roomList = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getStatus, RoomStatusEnum.AVAILABLE)
                .eq(MeetingRoom::getIsDelete, 0)
                .orderByAsc(MeetingRoom::getBuilding)
                .orderByAsc(MeetingRoom::getFloor)
                .orderByAsc(MeetingRoom::getRoomNumber)
                .list();
        return ResultUtils.success(roomList);
    }

    // endregion

    /**
     校验会议室请求参数
     */
    private void validateRoomAddRequest(RoomAddRequest request) {
        if (request.getRoomName() == null || request.getRoomName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议室名称不能为空");
        }
        if (request.getBuilding() == null || request.getBuilding().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "楼栋不能为空");
        }
        if (request.getFloor() == null || request.getFloor() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "楼层必须大于0");
        }
        if (request.getRoomNumber() == null || request.getRoomNumber().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间号不能为空");
        }
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "容纳人数必须大于0");
        }
    }

    /**
     校验会议室请求参数
     */
    private void validateRoomUpdateRequest(RoomUpdateRequest request) {
        if (request.getRoomName() == null || request.getRoomName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议室名称不能为空");
        }
        if (request.getBuilding() == null || request.getBuilding().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "楼栋不能为空");
        }
        if (request.getFloor() == null || request.getFloor() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "楼层必须大于0");
        }
        if (request.getRoomNumber() == null || request.getRoomNumber().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "房间号不能为空");
        }
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "容纳人数必须大于0");
        }
    }
}
