package com.jjdx.bookmeeting.model.dto.admin.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingAddRequest {

    private String title;

    private Long roomId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long userId;

    private List<Long> attendeeIds;

    private Integer remindBefore;

    private String description;
}
