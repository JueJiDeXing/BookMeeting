package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.RemindTaskMapper;
import com.jjdx.bookmeeting.model.entity.RemindTask;
import com.jjdx.bookmeeting.model.enums.RemindStatusEnum;
import com.jjdx.bookmeeting.service.RemindTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 提醒任务服务实现
 */
@Service
@Slf4j
public class RemindTaskServiceImpl extends ServiceImpl<RemindTaskMapper, RemindTask> implements RemindTaskService {

    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;

    @Override
    public List<RemindTask> getByBookingId(Long bookingId) {
        LambdaQueryWrapper<RemindTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemindTask::getBookingId, bookingId)
                .orderByAsc(RemindTask::getRemindTime);
        return list(wrapper);
    }

    @Override
    public List<RemindTask> getByUserId(Long userId) {
        LambdaQueryWrapper<RemindTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemindTask::getUserId, userId)
                .orderByDesc(RemindTask::getRemindTime);
        return list(wrapper);
    }

    @Override
    public List<RemindTask> getPendingRemindTasks(LocalDateTime remindTime) {
        // 查询需要提醒的任务：
        // 1. 状态为待发送
        // 2. 提醒时间在当前时间前后5分钟内
        LocalDateTime start = remindTime.minusMinutes(5);
        LocalDateTime end = remindTime.plusMinutes(5);

        LambdaQueryWrapper<RemindTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemindTask::getStatus, RemindStatusEnum.PENDING.getValue())
                .between(RemindTask::getRemindTime, start, end);

        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreate(Long bookingId, List<Long> userIds, LocalDateTime remindTime, Integer remindType) {
        if (CollectionUtils.isEmpty(userIds)) {
            return true;
        }

        // 校验提醒时间
        if (remindTime.isBefore(LocalDateTime.now())) {
            log.warn("提醒时间已过，不创建提醒任务，bookingId: {}, remindTime: {}", bookingId, remindTime);
            return false;
        }

        // 批量创建提醒任务
        List<RemindTask> taskList = userIds.stream()
                .map(userId -> {
                    RemindTask task = new RemindTask();
                    task.setBookingId(bookingId);
                    task.setUserId(userId);
                    task.setRemindTime(remindTime);
                    task.setRemindType(remindType);
                    task.setStatus(RemindStatusEnum.PENDING.getValue());
                    task.setRetryCount(0);
                    return task;
                })
                .collect(Collectors.toList());

        boolean saved = saveBatch(taskList);
        if (saved) {
            log.info("为预定[{}]批量创建{}个提醒任务，提醒时间：{}", bookingId, userIds.size(), remindTime);
        }

        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByBookingId(Long bookingId) {
        LambdaQueryWrapper<RemindTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemindTask::getBookingId, bookingId);
        return remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long taskId, Integer status, String errorMsg) {
        RemindTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提醒任务不存在");
        }

        task.setStatus(status);
        task.setErrorMsg(errorMsg);

        return updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsSent(Long taskId) {
        RemindTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提醒任务不存在");
        }

        task.setStatus(RemindStatusEnum.SENT.getValue());
        task.setErrorMsg(null);

        boolean updated = updateById(task);
        if (updated) {
            log.debug("提醒任务[{}]标记为已发送", taskId);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsFailed(Long taskId, String errorMsg) {
        RemindTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提醒任务不存在");
        }

        task.setStatus(RemindStatusEnum.FAILED.getValue());
        task.setErrorMsg(errorMsg);
        task.setRetryCount(task.getRetryCount() + 1);

        boolean updated = updateById(task);
        if (updated) {
            log.warn("提醒任务[{}]发送失败，重试次数：{}，错误：{}", taskId, task.getRetryCount(), errorMsg);
        }

        return updated;
    }

    @Override
    public List<RemindTask> getTasksNeedRetry(int maxRetryCount) {
        LambdaQueryWrapper<RemindTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemindTask::getStatus, RemindStatusEnum.FAILED.getValue())
                .lt(RemindTask::getRetryCount, maxRetryCount)
                .orderByAsc(RemindTask::getRemindTime);

        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelByBookingId(Long bookingId) {
        LambdaQueryWrapper<RemindTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemindTask::getBookingId, bookingId)
                .eq(RemindTask::getStatus, RemindStatusEnum.PENDING.getValue());

        RemindTask updateTask = new RemindTask();
        updateTask.setStatus(RemindStatusEnum.CANCELLED.getValue());

        boolean updated = update(updateTask, wrapper);
        if (updated) {
            log.info("取消预定[{}]的所有待发送提醒任务", bookingId);
        }

        return updated;
    }

    /**
     发送提醒的定时任务方法（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void processPendingRemindTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<RemindTask> pendingTasks = getPendingRemindTasks(now);

        if (CollectionUtils.isEmpty(pendingTasks)) {
            return;
        }

        log.info("开始处理{}个待发送的提醒任务", pendingTasks.size());

        for (RemindTask task : pendingTasks) {
            try {
                // TODO: 实际发送提醒（邮件、站内信等）
                // 这里需要注入对应的发送服务
                // emailService.sendRemindEmail(task.getUserId(), task.getBookingId());
                // siteMessageService.sendRemindMessage(task.getUserId(), task.getBookingId());

                // 发送成功后标记为已发送
                markAsSent(task.getId());

                log.info("提醒任务[{}]发送成功", task.getId());
            } catch (Exception e) {
                log.error("提醒任务[{}]发送失败", task.getId(), e);

                // 标记为发送失败
                markAsFailed(task.getId(), e.getMessage());

                // 如果超过最大重试次数，记录日志
                if (task.getRetryCount() >= MAX_RETRY_COUNT) {
                    log.error("提醒任务[{}]超过最大重试次数，放弃发送", task.getId());
                }
            }
        }
    }
}
