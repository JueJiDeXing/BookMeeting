package com.jjdx.bookmeeting.model.dto.user.booking;

import lombok.Data;

import java.io.Serializable;

/**
 会议室预定列表查询请求
 */
@Data
public class RoomBookingsRequest implements Serializable {

    /**
     会议室ID
     */
    private Long roomId;

    /**
     日期（YYYY-MM-DD格式，不传则默认为今天）
     */
    private String date;

    private static final long serialVersionUID = 1L;
}
