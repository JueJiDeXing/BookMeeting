package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.MessageMapper;
import com.jjdx.bookmeeting.model.entity.Message;
import com.jjdx.bookmeeting.model.vo.MessageVO;
import com.jjdx.bookmeeting.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRemindMessage(Long userId, Long bookingId, String title, String content, Long remindTaskId) {
        Message message = new Message();
        message.setUserId(userId);
        message.setBookingId(bookingId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(0);  // 会议提醒
        message.setStatus(0); // 未读
        message.setRemindTaskId(remindTaskId);

        save(message);
        log.info("为用户[{}]创建提醒消息：{}", userId, title);
    }

    @Override
    public List<MessageVO> getUnreadMessages(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId)
                .eq(Message::getStatus, 0)
                .orderByDesc(Message::getCreateTime);

        return list(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageVO> getUserMessages(Long userId, int current, int pageSize) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId)
                .orderByDesc(Message::getCreateTime);

        // 分页查询
        int offset = (current - 1) * pageSize;
        List<Message> messages = baseMapper.selectList(
                wrapper.last("LIMIT " + offset + "," + pageSize)
        );

        return messages.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long messageId, Long userId) {
        Message message = getById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        }
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此消息");
        }
        if (message.getStatus() == 1) {
            return true; // 已读，直接返回
        }

        message.setStatus(1);
        message.setReadTime(LocalDateTime.now());
        return updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchMarkAsRead(List<Long> messageIds, Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Message::getId, messageIds)
                .eq(Message::getUserId, userId)
                .eq(Message::getStatus, 0);

        Message update = new Message();
        update.setStatus(1);
        update.setReadTime(LocalDateTime.now());

        return update(update, wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId)
                .eq(Message::getStatus, 0);
        return (int) count(wrapper);
    }

    private MessageVO toVO(Message message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
