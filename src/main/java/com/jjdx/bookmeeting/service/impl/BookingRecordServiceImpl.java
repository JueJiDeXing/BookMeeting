package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.constant.CommonConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.BookingRecordMapper;
import com.jjdx.bookmeeting.model.dto.admin.booking.BookingCalendarRequest;
import com.jjdx.bookmeeting.model.dto.admin.booking.BookingUpdateRequest;
import com.jjdx.bookmeeting.model.entity.*;
import com.jjdx.bookmeeting.model.enums.AttendeeResponseStatusEnum;
import com.jjdx.bookmeeting.model.enums.BookingStatusEnum;
import com.jjdx.bookmeeting.model.enums.RemindStatusEnum;
import com.jjdx.bookmeeting.model.enums.RemindTypeEnum;
import com.jjdx.bookmeeting.model.vo.AttendeeVO;
import com.jjdx.bookmeeting.model.vo.BookingVO;
import com.jjdx.bookmeeting.service.*;
import com.jjdx.bookmeeting.service.params.BookingAddParams;
import com.jjdx.bookmeeting.service.params.BookingQueryParams;
import com.jjdx.bookmeeting.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 预定记录服务实现
 */
@Service
@Slf4j
public class BookingRecordServiceImpl extends ServiceImpl<BookingRecordMapper, BookingRecord> implements BookingRecordService {

    @Resource
    @Lazy
    private MeetingRoomService meetingRoomService;

    @Resource
    private UserService userService;

    @Resource
    private AttendeeResponseService attendeeResponseService;

    @Resource
    private RemindTaskService remindTaskService;

    @Override
    public boolean checkRoomConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId) {
        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingRecord::getRoomId, roomId)
                .ne(excludeBookingId != null, BookingRecord::getId, excludeBookingId)
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING.getValue(),
                        BookingStatusEnum.IN_PROGRESS.getValue()) // 待签到和进行中的预定
                .and(w -> w.between(BookingRecord::getStartTime, startTime, endTime)
                        .or().between(BookingRecord::getEndTime, startTime, endTime)
                        .or().apply("start_time <= {0} and end_time >= {1}", startTime, endTime));

        return count(wrapper) > 0;
    }

    @Override
    public boolean checkUserHasActiveBooking(Long userId) {
        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingRecord::getUserId, userId)
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING.getValue(),
                        BookingStatusEnum.IN_PROGRESS.getValue()) // 待签到和进行中
                .gt(BookingRecord::getEndTime, LocalDateTime.now());

        return count(wrapper) > 0;
    }

    @Override
    public List<BookingRecord> getActiveBookingsByRoomId(Long roomId) {
        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingRecord::getRoomId, roomId)
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING.getValue(),
                        BookingStatusEnum.IN_PROGRESS.getValue()) // 待签到和进行中
                .orderByAsc(BookingRecord::getStartTime);

        return list(wrapper);
    }

    @Override
    public List<BookingRecord> getBookingsByUserId(Long userId, Integer status) {
        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingRecord::getUserId, userId)
                .eq(status != null, BookingRecord::getStatus, status)
                .orderByDesc(BookingRecord::getStartTime);

        return list(wrapper);
    }

    @Override
    public List<BookingRecord> getBookingsNeedRemind(LocalDateTime remindTime) {
        // 查询需要提醒的预定：
        // 1. 状态为待签到
        // 2. 提醒时间在当前时间前后5分钟内
        // 3. 还未发送提醒
        LocalDateTime start = remindTime.minusMinutes(5);
        LocalDateTime end = remindTime.plusMinutes(5);

        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingRecord::getStatus, BookingStatusEnum.PENDING.getValue()) // 待签到
                .between(BookingRecord::getStartTime, start, end);

        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBookingStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 将已过开始时间且未签到的预定状态更新为"未签到超时"
        LambdaQueryWrapper<BookingRecord> timeoutWrapper = new LambdaQueryWrapper<>();
        timeoutWrapper.eq(BookingRecord::getStatus, BookingStatusEnum.PENDING.getValue()) // 待签到
                .lt(BookingRecord::getStartTime, now);

        BookingRecord timeoutUpdate = new BookingRecord();
        timeoutUpdate.setStatus(BookingStatusEnum.TIMEOUT.getValue()); // 未签到超时
        update(timeoutUpdate, timeoutWrapper);

        // 2. 将已过结束时间的进行中预定更新为"已完成"
        LambdaQueryWrapper<BookingRecord> completeWrapper = new LambdaQueryWrapper<>();
        completeWrapper.eq(BookingRecord::getStatus, BookingStatusEnum.IN_PROGRESS.getValue()) // 进行中
                .lt(BookingRecord::getEndTime, now);

        BookingRecord completeUpdate = new BookingRecord();
        completeUpdate.setStatus(BookingStatusEnum.COMPLETED.getValue()); // 已完成
        update(completeUpdate, completeWrapper);

        log.info("定时任务：更新预定状态完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelBooking(Long bookingId, Long userId) {
        BookingRecord booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        // 只能取消自己的预定
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限取消该预定");
        }

        // 只能取消待签到的预定
        if (!BookingStatusEnum.isPending(booking.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能取消待签到的预定");
        }

        // 如果会议已经开始，不能取消
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议已开始，无法取消");
        }

        booking.setStatus(BookingStatusEnum.CANCELLED.getValue()); // 已取消
        return updateById(booking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeBooking(Long bookingId) {
        BookingRecord booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        booking.setStatus(BookingStatusEnum.COMPLETED.getValue()); // 已完成
        booking.setActualEnd(LocalDateTime.now());
        return updateById(booking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean signIn(Long bookingId, Long userId) {
        BookingRecord booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        // 检查是否为参会人员（简化版：只检查预定人）
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该会议的预定人");
        }

        // 检查会议状态
        if (!BookingStatusEnum.isPending(booking.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该会议无法签到");
        }

        // 检查签到时间（允许提前15分钟签到）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime signInStartTime = booking.getStartTime().minusMinutes(15);
        if (now.isBefore(signInStartTime) || now.isAfter(booking.getEndTime())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不在签到时间内");
        }

        booking.setStatus(BookingStatusEnum.IN_PROGRESS.getValue()); // 进行中
        booking.setActualStart(now);
        return updateById(booking);
    }

    @Override
    public int countBookingsByRoomId(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingRecord::getRoomId, roomId)
                .ge(startTime != null, BookingRecord::getStartTime, startTime)
                .le(endTime != null, BookingRecord::getEndTime, endTime)
                .in(BookingRecord::getStatus,
                        BookingStatusEnum.PENDING.getValue(),
                        BookingStatusEnum.IN_PROGRESS.getValue(),
                        BookingStatusEnum.COMPLETED.getValue()); // 待签到、进行中、已完成

        return (int) count(wrapper);
    }

    @Override
    public int countBookingsByUserId(Long userId) {
        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingRecord::getUserId, userId);

        return (int) count(wrapper);
    }

    // ==================== 新增方法 ====================

    // BookingRecordServiceImpl.java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingRecord addBooking(BookingAddParams params, Long operatorId) {
        // 1. 创建预定记录
        BookingRecord booking = new BookingRecord();
        BeanUtils.copyProperties(params, booking);

        // 设置默认值
        booking.setStatus(BookingStatusEnum.PENDING.getValue()); // 待签到
        if (booking.getRemindBefore() == null) {
            booking.setRemindBefore(15);
        }

        // 处理参会人员ID列表
        if (CollectionUtils.isNotEmpty(params.getAttendeeIds())) {
            if (!params.getAttendeeIds().contains(params.getUserId())) {
                params.getAttendeeIds().add(params.getUserId());
            }
            String attendeesIdStr = params.getAttendeeIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            booking.setAttendeesId(attendeesIdStr);
        } else {
            booking.setAttendeesId(String.valueOf(params.getUserId()));
        }

        boolean saved = save(booking);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建预定失败");
        }

        // 2. 创建参会人员响应记录
        createAttendeeResponses(booking.getId(), params.getAttendeeIds());

        // 3. 创建提醒任务
        createRemindTask(booking);

        log.info("用户[{}]创建预定[{}]，会议室[{}]，时间[{}-{}]",
                operatorId, booking.getId(), booking.getRoomId(),
                booking.getStartTime(), booking.getEndTime());

        return booking;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBooking(BookingUpdateRequest updateRequest, Long operatorId) {
        // 1. 获取原预定记录
        BookingRecord oldBooking = getById(updateRequest.getId());
        if (oldBooking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        // 2. 只能更新待签到的预定
        if (!BookingStatusEnum.isPending(oldBooking.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能更新待签到的预定");
        }

        // 3. 更新预定记录
        BookingRecord booking = new BookingRecord();
        BeanUtils.copyProperties(updateRequest, booking);
        booking.setId(updateRequest.getId());

        boolean updated = updateById(booking);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新预定失败");
        }

        // 4. 如果更新了参会人员
        if (updateRequest.getAttendeeIds() != null) {
            // 删除旧的参会人员响应
            attendeeResponseService.lambdaUpdate()
                    .eq(AttendeeResponse::getBookingId, updateRequest.getId())
                    .remove();

            // 创建新的参会人员响应
            createAttendeeResponses(updateRequest.getId(), updateRequest.getAttendeeIds());
        }

        // 5. 如果更新了时间或提醒设置，重新创建提醒任务
        if (updateRequest.getStartTime() != null ||
                updateRequest.getEndTime() != null ||
                updateRequest.getRemindBefore() != null) {

            // 删除旧的提醒任务
            remindTaskService.lambdaUpdate()
                    .eq(RemindTask::getBookingId, updateRequest.getId())
                    .remove();

            // 获取最新的预定记录
            BookingRecord latestBooking = getById(updateRequest.getId());
            // 创建新的提醒任务
            createRemindTask(latestBooking);
        }

        log.info("管理员[{}]更新预定[{}]", operatorId, updateRequest.getId());

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelBooking(Long bookingId, Long operatorId, String reason) {
        BookingRecord booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        // 只能取消待签到的预定
        if (!BookingStatusEnum.isPending(booking.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能取消待签到的预定");
        }

        // 如果会议已经开始，不能取消
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "会议已开始，无法取消");
        }

        // 更新状态为已取消
        booking.setStatus(BookingStatusEnum.CANCELLED.getValue()); // 已取消
        boolean updated = updateById(booking);

        if (updated) {
            // 删除相关的提醒任务
            remindTaskService.lambdaUpdate()
                    .eq(RemindTask::getBookingId, bookingId)
                    .remove();

            log.info("管理员[{}]取消预定[{}]，原因：{}", operatorId, bookingId, reason);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCancelBooking(List<Long> bookingIds, Long operatorId) {
        if (CollectionUtils.isEmpty(bookingIds)) {
            return true;
        }

        int successCount = 0;
        for (Long bookingId : bookingIds) {
            try {
                if (cancelBooking(bookingId, operatorId, "批量取消")) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量取消预定失败，bookingId: {}", bookingId, e);
            }
        }

        log.info("管理员[{}]批量取消预定，成功：{}，总数：{}", operatorId, successCount, bookingIds.size());
        return successCount == bookingIds.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeBooking(Long bookingId, Long operatorId) {
        BookingRecord booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        // 只能结束进行中的预定
        if (!BookingStatusEnum.isInProgress(booking.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能结束进行中的会议");
        }

        booking.setStatus(BookingStatusEnum.COMPLETED.getValue()); // 已完成
        booking.setActualEnd(LocalDateTime.now());

        boolean updated = updateById(booking);

        if (updated) {
            log.info("管理员[{}]手动结束会议[{}]", operatorId, bookingId);
        }

        return updated;
    }

    @Override
    public boolean sendRemind(Long bookingId, Long operatorId) {
        BookingRecord booking = getById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预定记录不存在");
        }

        // 只能提醒待签到的会议
        if (!BookingStatusEnum.isPending(booking.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能提醒待签到的会议");
        }

        // 查询待发送的提醒任务
        List<RemindTask> remindTasks = remindTaskService.lambdaQuery()
                .eq(RemindTask::getBookingId, bookingId)
                .eq(RemindTask::getStatus, RemindStatusEnum.PENDING.getValue()) // 待发送
                .list();

        if (CollectionUtils.isEmpty(remindTasks)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有待发送的提醒任务");
        }

        // 更新提醒任务状态为已发送
        for (RemindTask task : remindTasks) {
            task.setStatus(RemindStatusEnum.SENT.getValue()); // 已发送
            remindTaskService.updateById(task);

            // TODO: 实际发送提醒（邮件、站内信等）
            log.info("发送提醒：会议[{}] 给用户[{}]，提醒方式：{}",
                    bookingId, task.getUserId(), task.getRemindType());
        }

        log.info("管理员[{}]手动发送提醒[{}]", operatorId, bookingId);
        return true;
    }

    @Override
    public BookingVO getBookingVO(BookingRecord booking) {
        if (booking == null) {
            return null;
        }

        BookingVO bookingVO = new BookingVO();
        BeanUtils.copyProperties(booking, bookingVO);

        // 获取会议室信息
        MeetingRoom room = meetingRoomService.getById(booking.getRoomId());
        if (room != null) {
            bookingVO.setRoomName(room.getRoomName());
            bookingVO.setRoomLocation(room.getLocationDesc());
            bookingVO.setRoomCapacity(room.getCapacity());
        }

        // 获取预定人信息
        User user = userService.getById(booking.getUserId());
        if (user != null) {
            bookingVO.setUserAccount(user.getUserAccount());
            bookingVO.setUserName(user.getUserName());
            bookingVO.setUserEmail(user.getEmail());
        }

        // 获取参会人员列表
        List<AttendeeResponse> attendeeResponses = attendeeResponseService.lambdaQuery()
                .eq(AttendeeResponse::getBookingId, booking.getId())
                .list();

        if (CollectionUtils.isNotEmpty(attendeeResponses)) {
            bookingVO.setAttendeeCount(attendeeResponses.size());

            List<AttendeeVO> attendeeVOList = attendeeResponses.stream()
                    .map(response -> {
                        AttendeeVO attendeeVO = new AttendeeVO();
                        BeanUtils.copyProperties(response, attendeeVO);

                        User attendee = userService.getById(response.getUserId());
                        if (attendee != null) {
                            attendeeVO.setUserAccount(attendee.getUserAccount());
                            attendeeVO.setUserName(attendee.getUserName());
                            attendeeVO.setEmail(attendee.getEmail());
                            attendeeVO.setPhone(attendee.getPhone());
                        }

                        return attendeeVO;
                    })
                    .collect(Collectors.toList());

            bookingVO.setAttendeeList(attendeeVOList);
        }

        // 计算会议时长（分钟）
        if (booking.getStartTime() != null && booking.getEndTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(booking.getStartTime(), booking.getEndTime());
            bookingVO.setDurationMinutes(minutes);
        }

        return bookingVO;
    }

    // BookingRecordServiceImpl.java

    @Override
    public QueryWrapper<BookingRecord> getQueryWrapper(BookingQueryParams params) {
        if (params == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = params.getId();
        String title = params.getTitle();
        Long roomId = params.getRoomId();
        String roomName = params.getRoomName();
        Long userId = params.getUserId();
        String userName = params.getUserName();
        Long attendeeId = params.getAttendeeId();
        Integer status = params.getStatus();
        List<Integer> statusList = params.getStatusList();
        LocalDateTime startTimeBegin = params.getStartTimeBegin();
        LocalDateTime startTimeEnd = params.getStartTimeEnd();
        LocalDateTime endTimeBegin = params.getEndTimeBegin();
        LocalDateTime endTimeEnd = params.getEndTimeEnd();
        LocalDateTime createTimeBegin = params.getCreateTimeBegin();
        LocalDateTime createTimeEnd = params.getCreateTimeEnd();
        Integer isDelete = params.getIsDelete();
        String sortField = params.getSortField();
        String sortOrder = params.getSortOrder();

        QueryWrapper<BookingRecord> queryWrapper = new QueryWrapper<>();

        // 精确查询
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(roomId != null, "room_id", roomId);
        queryWrapper.eq(userId != null, "user_id", userId);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.in(CollectionUtils.isNotEmpty(statusList), "status", statusList);
        queryWrapper.eq(isDelete != null, "is_delete", isDelete);

        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);

        // 如果有参会人员ID，需要通过联查attendee_response表
        if (attendeeId != null) {
            // 查询参会人员响应表获取该用户参与的所有预定ID
            List<AttendeeResponse> attendeeResponses = attendeeResponseService.lambdaQuery()
                    .eq(AttendeeResponse::getUserId, attendeeId)
                    .select(AttendeeResponse::getBookingId)
                    .list();

            if (CollectionUtils.isNotEmpty(attendeeResponses)) {
                List<Long> bookingIds = attendeeResponses.stream()
                        .map(AttendeeResponse::getBookingId)
                        .collect(Collectors.toList());
                queryWrapper.in("id", bookingIds);
            } else {
                // 没有找到任何预定，返回空结果
                queryWrapper.eq("id", 0);
            }
        }

        // 如果有会议室名称，需要通过联查meeting_room表
        if (StringUtils.isNotBlank(roomName)) {
            List<MeetingRoom> rooms = meetingRoomService.lambdaQuery()
                    .like(MeetingRoom::getRoomName, roomName)
                    .select(MeetingRoom::getId)
                    .list();

            if (CollectionUtils.isNotEmpty(rooms)) {
                List<Long> roomIds = rooms.stream()
                        .map(MeetingRoom::getId)
                        .collect(Collectors.toList());
                queryWrapper.in("room_id", roomIds);
            } else {
                queryWrapper.eq("id", 0);
            }
        }

        // 如果有预定人姓名，需要通过联查user表
        if (StringUtils.isNotBlank(userName)) {
            List<User> users = userService.lambdaQuery()
                    .like(User::getUserName, userName)
                    .select(User::getId)
                    .list();

            if (CollectionUtils.isNotEmpty(users)) {
                List<Long> userIds = users.stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
                queryWrapper.in("user_id", userIds);
            } else {
                queryWrapper.eq("id", 0);
            }
        }

        // 时间范围查询
        if (startTimeBegin != null && startTimeEnd != null) {
            queryWrapper.between("start_time", startTimeBegin, startTimeEnd);
        } else if (startTimeBegin != null) {
            queryWrapper.ge("start_time", startTimeBegin);
        } else if (startTimeEnd != null) {
            queryWrapper.le("start_time", startTimeEnd);
        }

        if (endTimeBegin != null && endTimeEnd != null) {
            queryWrapper.between("end_time", endTimeBegin, endTimeEnd);
        } else if (endTimeBegin != null) {
            queryWrapper.ge("end_time", endTimeBegin);
        } else if (endTimeEnd != null) {
            queryWrapper.le("end_time", endTimeEnd);
        }

        if (createTimeBegin != null && createTimeEnd != null) {
            queryWrapper.between("create_time", createTimeBegin, createTimeEnd);
        } else if (createTimeBegin != null) {
            queryWrapper.ge("create_time", createTimeBegin);
        } else if (createTimeEnd != null) {
            queryWrapper.le("create_time", createTimeEnd);
        }

        // 排序
        if (SqlUtils.validSortField(sortField)) {
            queryWrapper.orderBy(true, CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        } else {
            // 默认按开始时间倒序
            queryWrapper.orderByDesc("start_time");
        }

        return queryWrapper;
    }

    @Override
    public List<BookingVO> getCalendarBookings(BookingCalendarRequest calendarRequest) {
        LocalDate startDate = calendarRequest.getStartDate();
        LocalDate endDate = calendarRequest.getEndDate();
        Long roomId = calendarRequest.getRoomId();
        Long userId = calendarRequest.getUserId();

        // 构建查询条件
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<BookingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(BookingRecord::getStartTime, startDateTime, endDateTime)
                .or().between(BookingRecord::getEndTime, startDateTime, endDateTime);

        if (roomId != null) {
            wrapper.eq(BookingRecord::getRoomId, roomId);
        }

        if (userId != null) {
            wrapper.eq(BookingRecord::getUserId, userId);
        }

        wrapper.orderByAsc(BookingRecord::getStartTime);

        List<BookingRecord> bookingList = list(wrapper);

        // 转换为VO
        return bookingList.stream()
                .map(this::getBookingVO)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     创建参会人员响应记录
     */
    private void createAttendeeResponses(Long bookingId, List<Long> attendeeIds) {
        if (CollectionUtils.isEmpty(attendeeIds)) {
            return;
        }

        List<AttendeeResponse> responseList = attendeeIds.stream()
                .map(attendeeId -> {
                    AttendeeResponse response = new AttendeeResponse();
                    response.setBookingId(bookingId);
                    response.setUserId(attendeeId);
                    response.setStatus(AttendeeResponseStatusEnum.PENDING.getValue()); // 待确认
                    return response;
                })
                .collect(Collectors.toList());

        attendeeResponseService.saveBatch(responseList);
    }

    /**
     创建提醒任务
     */
    private void createRemindTask(BookingRecord booking) {
        if (booking.getRemindBefore() == null || booking.getRemindBefore() <= 0) {
            return; // 不需要提醒
        }

        // 计算提醒时间
        LocalDateTime remindTime = booking.getStartTime().minusMinutes(booking.getRemindBefore());

        // 如果提醒时间已过，不再创建
        if (remindTime.isBefore(LocalDateTime.now())) {
            return;
        }

        // 解析参会人员ID
        List<Long> attendeeIds = parseAttendeeIds(booking.getAttendeesId());

        // 为每个参会人员创建提醒任务
        List<RemindTask> remindTasks = attendeeIds.stream()
                .map(attendeeId -> {
                    RemindTask task = new RemindTask();
                    task.setBookingId(booking.getId());
                    task.setUserId(attendeeId);
                    task.setRemindTime(remindTime);
                    task.setRemindType(RemindTypeEnum.ALL.getValue()); // 默认全部方式提醒
                    task.setStatus(RemindStatusEnum.PENDING.getValue()); // 待发送
                    task.setRetryCount(0);
                    return task;
                })
                .collect(Collectors.toList());

        remindTaskService.saveBatch(remindTasks);

        log.info("为预定[{}]创建提醒任务{}个，提醒时间：{}", booking.getId(), remindTasks.size(), remindTime);
    }

    /**
     解析参会人员ID字符串
     */
    private List<Long> parseAttendeeIds(String attendeesIdStr) {
        List<Long> attendeeIds = new ArrayList<>();
        if (StringUtils.isNotBlank(attendeesIdStr)) {
            String[] idArray = attendeesIdStr.split(",");
            for (String id : idArray) {
                try {
                    attendeeIds.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException e) {
                    log.error("解析参会人员ID失败: {}", id, e);
                }
            }
        }
        return attendeeIds;
    }
}
