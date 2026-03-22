package com.jjdx.bookmeeting.model.dto.user.booking;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户端签到请求
 */
@Data
public class UserBookingSignInRequest implements Serializable {

    /**
     * 预定记录ID
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
