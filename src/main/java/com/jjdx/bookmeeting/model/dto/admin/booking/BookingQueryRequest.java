package com.jjdx.bookmeeting.model.dto.admin.booking;

import com.jjdx.bookmeeting.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预定查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BookingQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 参会人员ID（查询该人员参与的所有会议）
     */
    private Long attendeeId;

    /**
     * 会议状态列表
     */
    private List<Integer> statusList;

    /**
     * 会议状态
     */
    private Integer status;

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
}
