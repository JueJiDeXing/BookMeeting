package com.jjdx.bookmeeting.scheduler;

import com.jjdx.bookmeeting.model.entity.BookingRecord;
import com.jjdx.bookmeeting.model.entity.RemindTask;
import com.jjdx.bookmeeting.model.enums.BookingStatusEnum;
import com.jjdx.bookmeeting.model.enums.RemindStatusEnum;
import com.jjdx.bookmeeting.service.BookingRecordService;
import com.jjdx.bookmeeting.service.MessageService;
import com.jjdx.bookmeeting.service.RemindTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class RemindScheduler {

    @Resource
    private RemindTaskService remindTaskService;

    @Resource
    private BookingRecordService bookingRecordService;

    @Resource
    private MessageService messageService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /**
     每分钟执行一次，检查并发送提醒
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processRemindTasks() {
        LocalDateTime now = LocalDateTime.now();

        // 查询待发送且提醒时间已到的任务
        List<RemindTask> pendingTasks = remindTaskService.lambdaQuery()
                .eq(RemindTask::getStatus, RemindStatusEnum.PENDING.getValue())
                .le(RemindTask::getRemindTime, now)
                .list();

        if (pendingTasks.isEmpty()) {
            return;
        }

        log.info("开始处理 {} 条待发送提醒", pendingTasks.size());

        for (RemindTask task : pendingTasks) {
            try {
                // 获取预定信息
                BookingRecord booking = bookingRecordService.getById(task.getBookingId());
                if (booking == null) {
                    log.warn("提醒任务[{}]关联的预定不存在", task.getId());
                    // 标记任务为失败
                    task.setStatus(RemindStatusEnum.FAILED.getValue());
                    task.setErrorMsg("预定不存在");
                    remindTaskService.updateById(task);
                    continue;
                }

                // 如果预定已完成或已取消，不再发送提醒
                if (BookingStatusEnum.isCompleted(booking.getStatus())
                        || BookingStatusEnum.isCancelled(booking.getStatus())) {
                    log.info("预定[{}]已取消/完成，跳过提醒", task.getBookingId());
                    task.setStatus(RemindStatusEnum.CANCELLED.getValue());
                    remindTaskService.updateById(task);
                    continue;
                }

                // 构建消息内容
                String startTimeStr = booking.getStartTime().format(TIME_FORMATTER);
                String endTimeStr = booking.getEndTime().format(TIME_FORMATTER);
                String title = "📅 会议提醒：" + booking.getTitle();
                String content = String.format(
                        "您有一个会议即将开始：\n会议室：%s\n时间：%s - %s\n主题：%s",
                        getRoomName(booking.getRoomId()),
                        startTimeStr,
                        endTimeStr,
                        booking.getTitle()
                );

                // 创建站内消息
                messageService.createRemindMessage(
                        task.getUserId(),
                        task.getBookingId(),
                        title,
                        content,
                        task.getId()
                );

                // 更新提醒任务状态为已发送
                task.setStatus(RemindStatusEnum.SENT.getValue());
                remindTaskService.updateById(task);

                log.info("提醒任务[{}]已发送给用户[{}]，会议[{}]",
                        task.getId(), task.getUserId(), task.getBookingId());

            } catch (Exception e) {
                log.error("处理提醒任务[{}]失败", task.getId(), e);
                task.setStatus(RemindStatusEnum.FAILED.getValue());
                task.setErrorMsg(e.getMessage());
                remindTaskService.updateById(task);
            }
        }
    }

    /**
     获取会议室名称（需要注入 MeetingRoomService）
     */
    private String getRoomName(Long roomId) {
        // TODO: 注入 MeetingRoomService 并查询
        return String.valueOf(roomId);
    }
}
