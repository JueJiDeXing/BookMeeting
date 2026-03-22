package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.entity.RemindTask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒任务服务接口
 */
public interface RemindTaskService extends IService<RemindTask> {

    /**
     * 根据预定ID获取提醒任务
     */
    List<RemindTask> getByBookingId(Long bookingId);

    /**
     * 根据用户ID获取提醒任务
     */
    List<RemindTask> getByUserId(Long userId);

    /**
     * 获取待发送的提醒任务
     *
     * @param remindTime 提醒时间（在此时间前后的任务）
     */
    List<RemindTask> getPendingRemindTasks(LocalDateTime remindTime);

    /**
     * 批量创建提醒任务
     *
     * @param bookingId   预定ID
     * @param userIds     用户ID列表
     * @param remindTime  提醒时间
     * @param remindType  提醒方式
     */
    boolean batchCreate(Long bookingId, List<Long> userIds, LocalDateTime remindTime, Integer remindType);

    /**
     * 根据预定ID删除所有提醒任务
     */
    boolean deleteByBookingId(Long bookingId);

    /**
     * 更新提醒任务状态
     *
     * @param taskId 任务ID
     * @param status 状态
     * @param errorMsg 错误信息
     */
    boolean updateStatus(Long taskId, Integer status, String errorMsg);

    /**
     * 标记任务为已发送
     */
    boolean markAsSent(Long taskId);

    /**
     * 标记任务为发送失败（并增加重试次数）
     */
    boolean markAsFailed(Long taskId, String errorMsg);

    /**
     * 获取需要重试的任务（发送失败且重试次数小于最大重试次数）
     */
    List<RemindTask> getTasksNeedRetry(int maxRetryCount);

    /**
     * 取消预定的所有提醒任务
     */
    boolean cancelByBookingId(Long bookingId);
}
