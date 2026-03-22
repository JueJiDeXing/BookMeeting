package com.jjdx.bookmeeting.model.dto.admin.booking;

import lombok.Data;

import java.io.Serializable;

/**
 * 预定提醒请求
 */
@Data
public class BookingRemindRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预定记录ID
     */
    private Long id;

    /**
     * 提醒方式（0-站内信 1-邮件 2-全部）
     */
    private Integer remindType;
}
