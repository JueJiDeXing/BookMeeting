package com.jjdx.bookmeeting.service.params;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 创建预定的参数对象
 */
@Data
public class BookingAddParams implements Serializable {

    private Long roomId;

    private Long userId;

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private List<Long> attendeeIds;

    private Integer remindBefore;

    private String description;

    private static final long serialVersionUID = 1L;
}
