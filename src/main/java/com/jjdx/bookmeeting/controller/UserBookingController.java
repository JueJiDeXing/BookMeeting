package com.jjdx.bookmeeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.dto.user.DateRangeRequest;
import com.jjdx.bookmeeting.model.dto.user.booking.*;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.enums.BookingStatusEnum;
import com.jjdx.bookmeeting.model.vo.BookingVO;
import com.jjdx.bookmeeting.service.BookingRecordService;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 用户端-预定接口
 */
@RestController
@RequestMapping("/user/booking")
@Slf4j
public class UserBookingController {

    @Resource
    private BookingRecordService bookingRecordService;

    @Resource
    private UserService userService;

    /**
     创建预定
     */
    @PostMapping("/add")
    public BaseResponse<Long> addBooking(@RequestBody UserBookingAddRequest addRequest,
                                         HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        Long userId = userService.getLoginUser(request).getId();
        addRequest.setUserId(userId);

        // 校验参数
        validateBookingAddRequest(addRequest);

        // 检查时间是否合理
        if (addRequest.getStartTime().isAfter(addRequest.getEndTime()) ||
                addRequest.getStartTime().isEqual(addRequest.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间必须小于结束时间");
        }

        if (addRequest.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能预定过去的时间");
        }

        // 检查时间冲突
        boolean hasConflict = bookingRecordService.checkRoomConflict(
                addRequest.getRoomId(),
                addRequest.getStartTime(),
                addRequest.getEndTime(),
                null
        );
        if (hasConflict) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该时间段会议室已被预定");
        }

        BookingRecord booking = bookingRecordService.addBooking(addRequest, userId);
        return ResultUtils.success(booking.getId());
    }

    /**
     取消预定
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> cancelBooking(@RequestBody UserBookingCancelRequest cancelRequest,
                                               HttpServletRequest request) {
        if (cancelRequest == null || cancelRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(request).getId();
        boolean cancelled = bookingRecordService.cancelBooking(cancelRequest.getId(), userId);
        return ResultUtils.success(cancelled);
    }

    /**
     签到
     */
    @PostMapping("/signin")
    public BaseResponse<Boolean> signIn(@RequestBody UserBookingSignInRequest signInRequest,
                                        HttpServletRequest request) {
        if (signInRequest == null || signInRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(request).getId();
        boolean signed = bookingRecordService.signIn(signInRequest.getId(), userId);
        return ResultUtils.success(signed);
    }

    /**
     结束会议
     */
    @PostMapping("/complete")
    public BaseResponse<Boolean> completeBooking(@RequestBody UserBookingCompleteRequest completeRequest,
                                                 HttpServletRequest request) {
        if (completeRequest == null || completeRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(request).getId();
        BookingRecord booking = bookingRecordService.getById(completeRequest.getId());

        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定不存在");
        }

        // 只能结束自己的预定
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限结束该会议");
        }

        // 只能结束进行中的会议
        if (booking.getStatus() != BookingStatusEnum.IN_PROGRESS.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能结束进行中的会议");
        }

        boolean completed = bookingRecordService.completeBooking(completeRequest.getId());
        return ResultUtils.success(completed);
    }

    /**
     检查时间是否可用
     */
    @PostMapping("/check/availability")
    public BaseResponse<Boolean> checkAvailability(@RequestBody UserBookingCheckRequest checkRequest,
                                                   HttpServletRequest request) {
        if (checkRequest == null || checkRequest.getRoomId() == null ||
                checkRequest.getStartTime() == null || checkRequest.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean hasConflict = bookingRecordService.checkRoomConflict(
                checkRequest.getRoomId(),
                checkRequest.getStartTime(),
                checkRequest.getEndTime(),
                null
        );

        return ResultUtils.success(hasConflict);
    }

    @PostMapping("/list/by-date")
    public BaseResponse<List<BookingRecord>> listBookingsByDate(@RequestBody DateRangeRequest request) {
        if (request == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<BookingRecord> bookings = bookingRecordService.lambdaQuery()
                .between(BookingRecord::getStartTime, request.getStartTime(), request.getEndTime())
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING,
                        BookingStatusEnum.IN_PROGRESS,
                        BookingStatusEnum.COMPLETED) // 待签到、进行中、已完成
                .list();

        return ResultUtils.success(bookings);
    }

    /**
     分页获取当前用户的预定列表
     */
    @PostMapping("/list/my/page")
    public BaseResponse<Page<BookingVO>> listMyBookings(@RequestBody UserBookingQueryRequest queryRequest,
                                                        HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(request).getId();
        queryRequest.setUserId(userId);

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        Page<BookingRecord> bookingPage = bookingRecordService.page(
                new Page<>(current, size),
                bookingRecordService.getQueryWrapper(queryRequest)
        );

        Page<BookingVO> bookingVOPage = new Page<>(current, size, bookingPage.getTotal());
        List<BookingVO> bookingVOList = bookingPage.getRecords().stream()
                .map(booking -> bookingRecordService.getBookingVO(booking))
                .collect(Collectors.toList());
        bookingVOPage.setRecords(bookingVOList);

        return ResultUtils.success(bookingVOPage);
    }

    /**
     根据ID获取预定详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<BookingVO> getBookingVOById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(request).getId();
        BookingRecord booking = bookingRecordService.getById(id);

        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定不存在");
        }

        // 只能查看自己的预定
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该预定");
        }

        BookingVO bookingVO = bookingRecordService.getBookingVO(booking);
        return ResultUtils.success(bookingVO);
    }

    /**
     获取某个会议室当天的预定列表
     */
    @PostMapping("/list/by-room")
    public BaseResponse<List<BookingVO>> listBookingsByRoomId(@RequestBody RoomBookingsRequest roomBookingsRequest,
                                                              HttpServletRequest request) {
        if (roomBookingsRequest == null || roomBookingsRequest.getRoomId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议室ID不能为空");
        }

        LocalDateTime startTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endTime = startTime.plusDays(1).minusSeconds(1);

        if (roomBookingsRequest.getDate() != null) {
            startTime = LocalDateTime.parse(roomBookingsRequest.getDate() + "T00:00:00");
            endTime = startTime.plusDays(1).minusSeconds(1);
        }

        List<BookingRecord> bookings = bookingRecordService.lambdaQuery()
                .eq(BookingRecord::getRoomId, roomBookingsRequest.getRoomId())
                .between(BookingRecord::getStartTime, startTime, endTime)
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING,
                        BookingStatusEnum.IN_PROGRESS,
                        BookingStatusEnum.COMPLETED)
                .orderByAsc(BookingRecord::getStartTime)
                .list();

        List<BookingVO> bookingVOList = bookings.stream()
                .map(booking -> bookingRecordService.getBookingVO(booking))
                .collect(Collectors.toList());

        return ResultUtils.success(bookingVOList);
    }

    private void validateBookingAddRequest(UserBookingAddRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议主题不能为空");
        }
        if (request.getTitle().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议主题不能超过100个字符");
        }
        if (request.getRoomId() == null || request.getRoomId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择会议室");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先登录");
        }
        if (request.getStartTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择开始时间");
        }
        if (request.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择结束时间");
        }

        // 校验时间是否为30分钟的倍数
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();

        if (start.getMinute() != 0 && start.getMinute() != 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间必须为整点或半点（如 14:00 或 14:30）");
        }
        if (end.getMinute() != 0 && end.getMinute() != 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间必须为整点或半点（如 15:00 或 15:30）");
        }

        if (start.getSecond() != 0 || start.getNano() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间秒数必须为0");
        }
        if (end.getSecond() != 0 || end.getNano() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间秒数必须为0");
        }

        long minutes = java.time.Duration.between(start, end).toMinutes();
        if (minutes % 30 != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议时长必须为30分钟的倍数");
        }
        if (minutes < 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议时长不能少于30分钟");
        }

        if (request.getRemindBefore() != null && (request.getRemindBefore() < 0 || request.getRemindBefore() > 24 * 60)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提醒时间必须在0-24小时之间");
        }
    }
}
