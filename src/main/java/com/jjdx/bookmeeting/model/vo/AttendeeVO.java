package com.jjdx.bookmeeting.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendeeVO {

    private Long id;

    private Long bookingId;

    private Long userId;

    private Integer status;  // 0-待确认 1-已确认 2-已拒绝

    private LocalDateTime responseTime;

    private String remark;

    // 用户信息
    private String userAccount;

    private String userName;

    private String email;

    private String phone;

    // ✅ 新增：会议信息
    private String bookingTitle;

    private LocalDateTime bookingStartTime;

    private LocalDateTime bookingEndTime;

    private Long roomId;
}
