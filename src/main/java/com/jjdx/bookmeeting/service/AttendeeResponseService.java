package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.entity.AttendeeResponse;

import java.util.List;

/**
 * 参会人员响应服务接口
 */
public interface AttendeeResponseService extends IService<AttendeeResponse> {

    /**
     * 根据预定ID获取参会人员响应列表
     */
    List<AttendeeResponse> getByBookingId(Long bookingId);

    /**
     * 根据用户ID获取参会记录
     */
    List<AttendeeResponse> getByUserId(Long userId);

    /**
     * 更新参会人员响应状态
     *
     * @param bookingId 预定ID
     * @param userId    用户ID
     * @param status    状态（0-待确认 1-已确认 2-已拒绝）
     * @param remark    备注（如拒绝原因）
     */
    boolean updateStatus(Long bookingId, Long userId, Integer status, String remark);

    /**
     * 批量创建参会人员响应
     *
     * @param bookingId   预定ID
     * @param attendeeIds 参会人员ID列表
     */
    boolean batchCreate(Long bookingId, List<Long> attendeeIds);

    /**
     * 根据预定ID删除所有参会人员响应
     */
    boolean deleteByBookingId(Long bookingId);

    /**
     * 统计预定已确认人数
     */
    int countConfirmedByBookingId(Long bookingId);

    /**
     * 检查用户是否已响应
     */
    boolean hasResponded(Long bookingId, Long userId);

    /**
     * 获取未响应的用户ID列表
     */
    List<Long> getUnrespondedUserIds(Long bookingId);
}
