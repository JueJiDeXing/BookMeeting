package com.jjdx.bookmeeting.model.dto.admin.booking;

import lombok.Data;

import java.io.Serializable;

/**
 预定取消请求
 */
@Data
public class BookingCancelRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     预定记录ID
     */
    private Long id;

    /**
     取消原因
     */
    private String reason;
}
