package com.jjdx.bookmeeting.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 批量标记消息已读请求
 */
@Data
public class MessageBatchReadRequest implements Serializable {

    /**
     消息ID列表
     */
    private List<Long> ids;

    private static final long serialVersionUID = 1L;
}
