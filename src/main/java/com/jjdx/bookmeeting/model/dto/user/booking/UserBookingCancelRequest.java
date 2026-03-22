package com.jjdx.bookmeeting.model.dto.user.booking;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户端取消预定请求
 */
@Data
public class UserBookingCancelRequest implements Serializable {

    /**
     * 预定记录ID
     */
    private Long id;

    /**
     * 取消原因
     */
    private String reason;

    private static final long serialVersionUID = 1L;
}
