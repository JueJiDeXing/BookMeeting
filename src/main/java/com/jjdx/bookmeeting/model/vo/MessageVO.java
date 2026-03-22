package com.jjdx.bookmeeting.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private Long userId;
    private Long bookingId;
    private String title;
    private String content;
    private Integer type;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime readTime;
}
