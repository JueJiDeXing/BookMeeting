package com.jjdx.bookmeeting.model.dto.user.booking;

import com.jjdx.bookmeeting.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户端预定查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserBookingQueryRequest extends PageRequest implements Serializable {

    /**
     * 预定记录ID
     */
    private Long id;

    /**
     * 会议标题（模糊查询）
     */
    private String title;

    /**
     * 会议室ID
     */
    private Long roomId;

    /**
     * 用户ID（后端自动填充）
     */
    private Long userId;

    /**
     * 会议状态（0-待签到 1-进行中 2-已完成 3-已取消 4-未签到超时）
     */
    private Integer status;

    /**
     * 状态列表
     */
    private List<Integer> statusList;

    /**
     * 开始时间范围-开始
     */
    private LocalDateTime startTimeBegin;

    /**
     * 开始时间范围-结束
     */
    private LocalDateTime startTimeEnd;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（asc/desc）
     */
    private String sortOrder;

    private static final long serialVersionUID = 1L;
}
