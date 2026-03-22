// BookingRecordService.java
package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.dto.admin.booking.BookingAddRequest;
import com.jjdx.bookmeeting.model.dto.admin.booking.BookingCalendarRequest;
import com.jjdx.bookmeeting.model.dto.admin.booking.BookingQueryRequest;
import com.jjdx.bookmeeting.model.dto.admin.booking.BookingUpdateRequest;
import com.jjdx.bookmeeting.model.dto.user.booking.UserBookingAddRequest;
import com.jjdx.bookmeeting.model.dto.user.booking.UserBookingQueryRequest;
import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.vo.BookingVO;
import com.jjdx.bookmeeting.service.params.BookingAddParams;
import com.jjdx.bookmeeting.service.params.BookingQueryParams;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预定记录服务
 */
public interface BookingRecordService extends IService<BookingRecord> {

    /**
     * 添加预定（使用参数对象）
     */
    BookingRecord addBooking(BookingAddParams params, Long operatorId);

    /**
     * 管理员端添加预定
     */
    default BookingRecord addBooking(BookingAddRequest addRequest, Long operatorId) {
        BookingAddParams params = new BookingAddParams();
        BeanUtils.copyProperties(addRequest, params);
        return addBooking(params, operatorId);
    }

    /**
     * 用户端添加预定
     */
    default BookingRecord addBooking(UserBookingAddRequest addRequest, Long userId) {
        BookingAddParams params = new BookingAddParams();
        BeanUtils.copyProperties(addRequest, params);
        params.setUserId(userId);
        return addBooking(params, userId);
    }
    /**
     * 更新预定
     */
    boolean updateBooking(BookingUpdateRequest updateRequest, Long operatorId);

    /**
     * 取消预定
     */
    boolean cancelBooking(Long bookingId, Long operatorId, String reason);

    /**
     * 批量取消预定
     */
    boolean batchCancelBooking(List<Long> bookingIds, Long operatorId);

    /**
     * 完成预定
     */
    boolean completeBooking(Long bookingId, Long operatorId);

    /**
     * 发送提醒
     */
    boolean sendRemind(Long bookingId, Long operatorId);

    /**
     * 获取预定VO
     */
    BookingVO getBookingVO(BookingRecord booking);

    // BookingRecordService.java

    /**
     * 获取查询条件包装器（使用通用参数）
     */
    QueryWrapper<BookingRecord> getQueryWrapper(BookingQueryParams params);

    /**
     * 管理员端获取查询条件包装器
     */
    default QueryWrapper<BookingRecord> getQueryWrapper(BookingQueryRequest request) {
        BookingQueryParams params = new BookingQueryParams();
        BeanUtils.copyProperties(request, params);
        return getQueryWrapper(params);
    }

    /**
     * 用户端获取查询条件包装器
     */
    default QueryWrapper<BookingRecord> getQueryWrapper(UserBookingQueryRequest request) {
        BookingQueryParams params = new BookingQueryParams();
        BeanUtils.copyProperties(request, params);
        params.setUserId(request.getUserId()); // 用户端固定查询自己的预定
        return getQueryWrapper(params);
    }

    /**
     * 获取日历预定数据
     */
    List<BookingVO> getCalendarBookings(BookingCalendarRequest calendarRequest);
    /**
     * 检查会议室在指定时间段内是否有冲突
     */
    boolean checkRoomConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId);

    /**
     * 检查用户是否有未完成的预定
     */
    boolean checkUserHasActiveBooking(Long userId);

    /**
     * 获取会议室的进行中预定
     */
    List<BookingRecord> getActiveBookingsByRoomId(Long roomId);

    /**
     * 获取用户的预定列表
     */
    List<BookingRecord> getBookingsByUserId(Long userId, Integer status);

    /**
     * 获取待提醒的预定
     */
    List<BookingRecord> getBookingsNeedRemind(LocalDateTime remindTime);

    /**
     * 更新预定状态（自动任务使用）
     */
    void updateBookingStatus();

    /**
     * 取消预定
     */
    boolean cancelBooking(Long bookingId, Long userId);

    /**
     * 完成预定
     */
    boolean completeBooking(Long bookingId);

    /**
     * 签到
     */
    boolean signIn(Long bookingId, Long userId);

    /**
     * 获取会议室的预定统计
     */
    int countBookingsByRoomId(Long roomId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户预定次数
     */
    int countBookingsByUserId(Long userId);
}
