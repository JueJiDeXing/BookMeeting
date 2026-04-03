package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.entity.Message;
import com.jjdx.bookmeeting.model.vo.MessageVO;

import java.util.List;

public interface MessageService extends IService<Message> {

    /**
     为指定用户创建提醒消息
     */
    void createRemindMessage(Long userId, Long bookingId, String title, String content, Long remindTaskId);

    /**
     获取用户未读消息列表
     */
    List<MessageVO> getUnreadMessages(Long userId);

    /**
     获取用户所有消息（分页）
     */
    List<MessageVO> getUserMessages(Long userId, int current, int pageSize);

    /**
     标记消息为已读
     */
    boolean markAsRead(Long messageId, Long userId);

    /**
     批量标记为已读
     */
    boolean batchMarkAsRead(List<Long> messageIds, Long userId);

    /**
     获取用户未读消息数量
     */
    int getUnreadCount(Long userId);
}
