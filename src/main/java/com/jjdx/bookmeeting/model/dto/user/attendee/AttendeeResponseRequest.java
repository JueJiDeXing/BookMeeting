package com.jjdx.bookmeeting.model.dto.user.attendee;

import lombok.Data;

import java.io.Serializable;

/**
 参会人员响应请求
 */
@Data
public class AttendeeResponseRequest implements Serializable {

    /**
     预定记录ID
     */
    private Long bookingId;

    /**
     响应状态（1-已确认 2-已拒绝）
     */
    private Integer status;

    /**
     备注（如拒绝原因）
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
