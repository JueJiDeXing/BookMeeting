package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.dto.user.attendee.AttendeeResponseRequest;
import com.jjdx.bookmeeting.model.entity.AttendeeResponse;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.vo.AttendeeVO;
import com.jjdx.bookmeeting.service.AttendeeResponseService;
import com.jjdx.bookmeeting.service.BookingRecordService;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户端-参会响应接口
 */
@RestController
@RequestMapping("/user/attendee")
@Slf4j
public class UserAttendeeController {

    @Resource
    private AttendeeResponseService attendeeResponseService;

    @Resource
    private BookingRecordService bookingRecordService;

    @Resource
    private UserService userService;

    /**
     * 获取用户待响应的会议列表
     */
    @GetMapping("/pending")
    public BaseResponse<List<AttendeeVO>> getPendingMeetings(HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();

        // 查询用户所有待响应的参会记录
        List<AttendeeResponse> responses = attendeeResponseService.lambdaQuery()
                .eq(AttendeeResponse::getUserId, userId)
                .eq(AttendeeResponse::getStatus, 0) // 待确认
                .eq(AttendeeResponse::getIsDelete, 0)
                .list();

        // 转换为VO，并关联会议信息
        List<AttendeeVO> result = responses.stream()
                .map(response -> {
                    BookingRecord booking = bookingRecordService.getById(response.getBookingId());
                    if (booking == null) {
                        return null;
                    }
                    // 只显示未开始的会议
                    if (booking.getStartTime().isBefore(java.time.LocalDateTime.now())) {
                        return null;
                    }
                    AttendeeVO vo = new AttendeeVO();
                    vo.setId(response.getId());
                    vo.setBookingId(response.getBookingId());
                    vo.setUserId(response.getUserId());
                    vo.setStatus(response.getStatus());
                    vo.setResponseTime(response.getResponseTime());
                    vo.setRemark(response.getRemark());
                    vo.setBookingTitle(booking.getTitle());
                    vo.setBookingStartTime(booking.getStartTime());
                    vo.setBookingEndTime(booking.getEndTime());
                    vo.setRoomId(booking.getRoomId());
                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(java.util.stream.Collectors.toList());

        return ResultUtils.success(result);
    }

    /**
     * 获取用户已响应的会议列表（已确认/已拒绝）
     */
    @GetMapping("/responded")
    public BaseResponse<List<AttendeeVO>> getRespondedMeetings(HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();

        List<AttendeeResponse> responses = attendeeResponseService.lambdaQuery()
                .eq(AttendeeResponse::getUserId, userId)
                .in(AttendeeResponse::getStatus, 1, 2) // 已确认、已拒绝
                .eq(AttendeeResponse::getIsDelete, 0)
                .orderByDesc(AttendeeResponse::getResponseTime)
                .list();

        List<AttendeeVO> result = responses.stream()
                .map(response -> {
                    BookingRecord booking = bookingRecordService.getById(response.getBookingId());
                    if (booking == null) {
                        return null;
                    }
                    AttendeeVO vo = new AttendeeVO();
                    vo.setId(response.getId());
                    vo.setBookingId(response.getBookingId());
                    vo.setUserId(response.getUserId());
                    vo.setStatus(response.getStatus());
                    vo.setResponseTime(response.getResponseTime());
                    vo.setRemark(response.getRemark());
                    vo.setBookingTitle(booking.getTitle());
                    vo.setBookingStartTime(booking.getStartTime());
                    vo.setBookingEndTime(booking.getEndTime());
                    vo.setRoomId(booking.getRoomId());
                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(java.util.stream.Collectors.toList());

        return ResultUtils.success(result);
    }

    /**
     * 确认参会
     */
    @PostMapping("/confirm")
    public BaseResponse<Boolean> confirmMeeting(@RequestBody AttendeeResponseRequest request,
                                                HttpServletRequest httpRequest) {
        if (request == null || request.getBookingId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(httpRequest).getId();

        // 检查预定是否存在
        BookingRecord booking = bookingRecordService.getById(request.getBookingId());
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议不存在");
        }

        // 检查会议是否已开始
        if (booking.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议已开始，无法确认参会");
        }

        // 检查会议是否已取消
        if (booking.getStatus() == 3) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议已取消");
        }

        // 更新响应状态
        boolean success = attendeeResponseService.updateStatus(
                request.getBookingId(),
                userId,
                1,  // 1-已确认
                request.getRemark()
        );

        if (success) {
            log.info("用户[{}]确认参会，会议[{}]", userId, request.getBookingId());
        }

        return ResultUtils.success(success);
    }

    /**
     * 拒绝参会
     */
    @PostMapping("/reject")
    public BaseResponse<Boolean> rejectMeeting(@RequestBody AttendeeResponseRequest request,
                                               HttpServletRequest httpRequest) {
        if (request == null || request.getBookingId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(httpRequest).getId();

        // 检查预定是否存在
        BookingRecord booking = bookingRecordService.getById(request.getBookingId());
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议不存在");
        }

        // 检查会议是否已开始
        if (booking.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议已开始，无法拒绝参会");
        }

        // 检查会议是否已取消
        if (booking.getStatus() == 3) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议已取消");
        }

        // 更新响应状态
        boolean success = attendeeResponseService.updateStatus(
                request.getBookingId(),
                userId,
                2,  // 2-已拒绝
                request.getRemark()
        );

        if (success) {
            log.info("用户[{}]拒绝参会，会议[{}]，原因：{}", userId, request.getBookingId(), request.getRemark());
        }

        return ResultUtils.success(success);
    }

    /**
     * 获取某个会议中当前用户的响应状态
     */
    @GetMapping("/status")
    public BaseResponse<Integer> getAttendeeStatus(@RequestParam Long bookingId,
                                                   HttpServletRequest request) {
        if (bookingId == null || bookingId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(request).getId();

        AttendeeResponse response = attendeeResponseService.lambdaQuery()
                .eq(AttendeeResponse::getBookingId, bookingId)
                .eq(AttendeeResponse::getUserId, userId)
                .one();

        if (response == null) {
            return ResultUtils.success(null);
        }

        return ResultUtils.success(response.getStatus());
    }

    /**
     * 批量响应（可选）
     */
    @PostMapping("/batch")
    public BaseResponse<Boolean> batchRespond(@RequestBody java.util.List<AttendeeResponseRequest> requests,
                                              HttpServletRequest httpRequest) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(httpRequest).getId();
        int successCount = 0;

        for (AttendeeResponseRequest request : requests) {
            if (request.getBookingId() == null || request.getStatus() == null) {
                continue;
            }
            if (request.getStatus() != 1 && request.getStatus() != 2) {
                continue;
            }

            try {
                boolean success = attendeeResponseService.updateStatus(
                        request.getBookingId(),
                        userId,
                        request.getStatus(),
                        request.getRemark()
                );
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量响应失败，bookingId: {}", request.getBookingId(), e);
            }
        }

        log.info("用户[{}]批量响应，成功：{}，总数：{}", userId, successCount, requests.size());
        return ResultUtils.success(successCount == requests.size());
    }
}
