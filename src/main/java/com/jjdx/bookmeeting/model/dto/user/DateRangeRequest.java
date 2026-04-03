package com.jjdx.bookmeeting.model.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DateRangeRequest {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
