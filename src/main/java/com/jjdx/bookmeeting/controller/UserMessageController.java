package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.dto.user.MessageBatchReadRequest;
import com.jjdx.bookmeeting.model.vo.MessageVO;
import com.jjdx.bookmeeting.service.MessageService;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user/message")
@Slf4j
public class UserMessageController {

    @Resource
    private MessageService messageService;

    @Resource
    private UserService userService;

    /**
     获取未读消息列表
     */
    @GetMapping("/unread")
    public BaseResponse<List<MessageVO>> getUnreadMessages(HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        List<MessageVO> messages = messageService.getUnreadMessages(userId);
        return ResultUtils.success(messages);
    }

    /**
     获取未读消息数量
     */
    @GetMapping("/unread/count")
    public BaseResponse<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        int count = messageService.getUnreadCount(userId);
        return ResultUtils.success(count);
    }

    /**
     获取消息列表（分页）
     */
    @PostMapping("/list")
    public BaseResponse<List<MessageVO>> getMessages(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        List<MessageVO> messages = messageService.getUserMessages(userId, current, pageSize);
        return ResultUtils.success(messages);
    }

    /**
     标记单条消息为已读
     */
    @PostMapping("/read/{id}")
    public BaseResponse<Boolean> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        boolean result = messageService.markAsRead(id, userId);
        return ResultUtils.success(result);
    }

    /**
     批量标记为已读
     */
    @PostMapping("/read/batch")
    public BaseResponse<Boolean> batchMarkAsRead(@RequestBody MessageBatchReadRequest request,
                                                 HttpServletRequest req) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = userService.getLoginUser(req).getId();
        boolean result = messageService.batchMarkAsRead(request.getIds(), userId);
        return ResultUtils.success(result);
    }
}
