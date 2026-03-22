package com.jjdx.bookmeeting.service.params;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预定记录查询参数
 */
@Data
public class BookingQueryParams implements Serializable {

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
     * 会议室名称（模糊查询）
     */
    private String roomName;

    /**
     * 预定人ID
     */
    private Long userId;

    /**
     * 预定人姓名（模糊查询）
     */
    private String userName;

    /**
     * 参会人ID（查询该用户参与的所有预定）
     */
    private Long attendeeId;

    /**
     * 会议状态
     */
    private Integer status;

    /**
     * 会议状态列表
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
     * 结束时间范围-开始
     */
    private LocalDateTime endTimeBegin;

    /**
     * 结束时间范围-结束
     */
    private LocalDateTime endTimeEnd;

    /**
     * 创建时间范围-开始
     */
    private LocalDateTime createTimeBegin;

    /**
     * 创建时间范围-结束
     */
    private LocalDateTime createTimeEnd;

    /**
     * 是否删除
     */
    private Integer isDelete;

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
