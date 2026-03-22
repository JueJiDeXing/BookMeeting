package com.jjdx.bookmeeting.model.dto.user.booking;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户结束会议请求
 */
@Data
public class UserBookingCompleteRequest implements Serializable {

    /**
     * 预定ID
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
