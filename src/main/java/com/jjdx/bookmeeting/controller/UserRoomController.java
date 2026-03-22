package com.jjdx.bookmeeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.exception.ThrowUtils;
import com.jjdx.bookmeeting.model.dto.user.room.UserRoomQueryRequest;
import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import com.jjdx.bookmeeting.model.vo.RoomVO;
import com.jjdx.bookmeeting.service.MeetingRoomService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端-会议室接口
 */
@RestController
@RequestMapping("/user/room")
@Slf4j
public class UserRoomController {

    @Resource
    private MeetingRoomService meetingRoomService;

    @PostMapping("/list/page")
    public BaseResponse<Page<RoomVO>> listAvailableRooms(@RequestBody UserRoomQueryRequest queryRequest,
                                                         HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        // 调用用户端的默认方法
        Page<MeetingRoom> roomPage = meetingRoomService.page(
                new Page<>(current, size),
                meetingRoomService.getQueryWrapper(queryRequest)
        );

        Page<RoomVO> roomVOPage = new Page<>(current, size, roomPage.getTotal());
        List<RoomVO> roomVOList = roomPage.getRecords().stream()
                .map(room -> meetingRoomService.getRoomVO(room))
                .collect(Collectors.toList());
        roomVOPage.setRecords(roomVOList);

        return ResultUtils.success(roomVOPage);
    }

    /**
     * 根据ID获取会议室详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<RoomVO> getRoomVOById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        MeetingRoom room = meetingRoomService.getById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议室不存在");
        }

        // 非可用状态不能查看详情（可选）
        if (room.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议室不可用");
        }

        RoomVO roomVO = meetingRoomService.getRoomVO(room);
        return ResultUtils.success(roomVO);
    }

    /**
     * 获取所有楼栋列表（用于筛选）
     */
    @GetMapping("/buildings")
    public BaseResponse<List<String>> getAllBuildings(HttpServletRequest request) {
        List<String> buildings = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getStatus, 0)
                .eq(MeetingRoom::getIsDelete, 0)
                .select(MeetingRoom::getBuilding)
                .groupBy(MeetingRoom::getBuilding)
                .list()
                .stream()
                .map(MeetingRoom::getBuilding)
                .collect(Collectors.toList());
        return ResultUtils.success(buildings);
    }

    /**
     * 根据楼栋获取楼层列表
     */
    @GetMapping("/floors")
    public BaseResponse<List<Integer>> getFloorsByBuilding(@RequestParam String building,
                                                           HttpServletRequest request) {
        if (StringUtils.isBlank(building)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<Integer> floors = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getBuilding, building)
                .eq(MeetingRoom::getStatus, 0)
                .eq(MeetingRoom::getIsDelete, 0)
                .select(MeetingRoom::getFloor)
                .groupBy(MeetingRoom::getFloor)
                .orderByAsc(MeetingRoom::getFloor)
                .list()
                .stream()
                .map(MeetingRoom::getFloor)
                .collect(Collectors.toList());

        return ResultUtils.success(floors);
    }
}
