package com.jjdx.bookmeeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.DeleteRequest;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.constant.UserConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.exception.ThrowUtils;
import com.jjdx.bookmeeting.interceptor.aop.annotation.AuthCheck;
import com.jjdx.bookmeeting.model.dto.admin.booking.*;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.vo.BookingVO;
import com.jjdx.bookmeeting.service.BookingRecordService;
import com.jjdx.bookmeeting.service.MeetingRoomService;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员-会议预定管理接口
 */
@RestController
@RequestMapping("/admin/booking")
@Slf4j
public class AdminBookingController {

    @Resource
    private BookingRecordService bookingRecordService;

    @Resource
    private MeetingRoomService meetingRoomService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建预定
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addBooking(@RequestBody BookingAddRequest addRequest,
                                         HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

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

        // 检查会议室是否存在
        if (meetingRoomService.getById(addRequest.getRoomId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会议室不存在");
        }

        // 检查用户是否存在
        if (userService.getById(addRequest.getUserId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定人不存在");
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

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        BookingRecord booking = bookingRecordService.addBooking(addRequest, operatorId);
        return ResultUtils.success(booking.getId());
    }

    /**
     * 删除预定
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBooking(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查预定是否存在
        BookingRecord booking = bookingRecordService.getById(deleteRequest.getId());
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        boolean removed = bookingRecordService.removeById(deleteRequest.getId());
        return ResultUtils.success(removed);
    }

    /**
     * 批量删除预定
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteBooking(@RequestBody BookingBatchDeleteRequest batchDeleteRequest,
                                                    HttpServletRequest request) {
        if (batchDeleteRequest == null || batchDeleteRequest.getIds() == null || batchDeleteRequest.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean removed = bookingRecordService.removeByIds(batchDeleteRequest.getIds());
        return ResultUtils.success(removed);
    }

    /**
     * 更新预定
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateBooking(@RequestBody BookingUpdateRequest updateRequest,
                                               HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查预定是否存在
        BookingRecord oldBooking = bookingRecordService.getById(updateRequest.getId());
        if (oldBooking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        // 校验参数
        validateBookingUpdateRequest(updateRequest);

        // 检查时间是否合理
        if (updateRequest.getStartTime() != null && updateRequest.getEndTime() != null) {
            if (updateRequest.getStartTime().isAfter(updateRequest.getEndTime()) ||
                    updateRequest.getStartTime().isEqual(updateRequest.getEndTime())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间必须小于结束时间");
            }
        }

        // 检查时间冲突（如果时间或会议室有变化）
        Long roomId = updateRequest.getRoomId() != null ? updateRequest.getRoomId() : oldBooking.getRoomId();
        LocalDateTime startTime = updateRequest.getStartTime() != null ? updateRequest.getStartTime() : oldBooking.getStartTime();
        LocalDateTime endTime = updateRequest.getEndTime() != null ? updateRequest.getEndTime() : oldBooking.getEndTime();

        if (!roomId.equals(oldBooking.getRoomId()) ||
                !startTime.equals(oldBooking.getStartTime()) ||
                !endTime.equals(oldBooking.getEndTime())) {

            boolean hasConflict = bookingRecordService.checkRoomConflict(
                    roomId,
                    startTime,
                    endTime,
                    updateRequest.getId()
            );
            if (hasConflict) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该时间段会议室已被预定");
            }
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean updated = bookingRecordService.updateBooking(updateRequest, operatorId);
        return ResultUtils.success(updated);
    }

    /**
     * 取消预定
     */
    @PostMapping("/cancel")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> cancelBooking(@RequestBody BookingCancelRequest cancelRequest,
                                               HttpServletRequest request) {
        if (cancelRequest == null || cancelRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean cancelled = bookingRecordService.cancelBooking(cancelRequest.getId(), operatorId, cancelRequest.getReason());
        return ResultUtils.success(cancelled);
    }

    /**
     * 批量取消预定
     */
    @PostMapping("/cancel/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchCancelBooking(@RequestBody BookingBatchCancelRequest batchCancelRequest,
                                                    HttpServletRequest request) {
        if (batchCancelRequest == null || batchCancelRequest.getIds() == null || batchCancelRequest.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean cancelled = bookingRecordService.batchCancelBooking(batchCancelRequest.getIds(), operatorId);
        return ResultUtils.success(cancelled);
    }

    /**
     * 手动签到
     */
    @PostMapping("/signin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> signIn(@RequestBody BookingSignInRequest signInRequest,
                                        HttpServletRequest request) {
        if (signInRequest == null || signInRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean signed = bookingRecordService.signIn(signInRequest.getId(), operatorId);
        return ResultUtils.success(signed);
    }

    /**
     * 手动结束会议
     */
    @PostMapping("/complete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> completeBooking(@RequestBody BookingCompleteRequest completeRequest,
                                                 HttpServletRequest request) {
        if (completeRequest == null || completeRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean completed = bookingRecordService.completeBooking(completeRequest.getId(), operatorId);
        return ResultUtils.success(completed);
    }

    /**
     * 发送提醒
     */
    @PostMapping("/remind")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> sendRemind(@RequestBody BookingRemindRequest remindRequest,
                                            HttpServletRequest request) {
        if (remindRequest == null || remindRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean reminded = bookingRecordService.sendRemind(remindRequest.getId(), operatorId);
        return ResultUtils.success(reminded);
    }

    /**
     * 根据 id 获取预定
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<BookingRecord> getBookingById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        BookingRecord booking = bookingRecordService.getById(id);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(booking);
    }

    /**
     * 根据 id 获取预定VO（带详细信息）
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<BookingVO> getBookingVOById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        BookingRecord booking = bookingRecordService.getById(id);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        BookingVO bookingVO = bookingRecordService.getBookingVO(booking);
        return ResultUtils.success(bookingVO);
    }

    /**
     * 分页获取预定列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<BookingRecord>> listBookingByPage(@RequestBody BookingQueryRequest queryRequest,
                                                               HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        Page<BookingRecord> bookingPage = bookingRecordService.page(
                new Page<>(current, size),
                bookingRecordService.getQueryWrapper(queryRequest)
        );

        return ResultUtils.success(bookingPage);
    }

    /**
     * 分页获取预定VO列表（带详细信息）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<BookingVO>> listBookingVOByPage(@RequestBody BookingQueryRequest queryRequest,
                                                             HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        // 限制爬虫
        

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
     * 获取用户预定日历数据
     */
    @PostMapping("/calendar")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<BookingVO>> getCalendarBookings(@RequestBody BookingCalendarRequest calendarRequest,
                                                             HttpServletRequest request) {
        if (calendarRequest == null || calendarRequest.getStartDate() == null || calendarRequest.getEndDate() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<BookingVO> bookingList = bookingRecordService.getCalendarBookings(calendarRequest);
        return ResultUtils.success(bookingList);
    }

    // endregion

    // region 参数校验

    /**
     * 校验预定创建请求
     */
    private void validateBookingAddRequest(BookingAddRequest request) {
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择预定人");
        }
        if (request.getStartTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择开始时间");
        }
        if (request.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择结束时间");
        }
        if (request.getRemindBefore() != null && (request.getRemindBefore() < 0 || request.getRemindBefore() > 1440)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提醒时间必须在0-1440分钟之间");
        }
    }

    /**
     * 校验预定更新请求
     */
    private void validateBookingUpdateRequest(BookingUpdateRequest request) {
        if (request.getTitle() != null) {
            if (request.getTitle().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议主题不能为空");
            }
            if (request.getTitle().length() > 100) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "会议主题不能超过100个字符");
            }
        }
        if (request.getRemindBefore() != null && (request.getRemindBefore() < 0 || request.getRemindBefore() > 1440)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提醒时间必须在0-1440分钟之间");
        }
    }

    // endregion
}
