package com.jjdx.bookmeeting.model.dto.admin.booking;

import lombok.Data;

import java.io.Serializable;

/**
 预定完成请求
 */
@Data
public class BookingCompleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     预定记录ID
     */
    private Long id;
}
