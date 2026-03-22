package com.jjdx.bookmeeting.model.vo;

import com.jjdx.bookmeeting.model.entity.BookingRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookingVO extends BookingRecord implements Serializable {

    @ApiModelProperty("会议室名称")
    private String roomName;

    @ApiModelProperty("会议室位置描述")
    private String roomLocation;

    @ApiModelProperty("会议室容纳人数")
    private Integer roomCapacity;

    @ApiModelProperty("预定人账号")
    private String userAccount;

    @ApiModelProperty("预定人姓名")
    private String userName;

    @ApiModelProperty("预定人邮箱")
    private String userEmail;

    @ApiModelProperty("参会人数")
    private Integer attendeeCount;

    @ApiModelProperty("参会人员列表")
    private List<AttendeeVO> attendeeList;

    @ApiModelProperty("会议时长（分钟）")
    private Long durationMinutes;

    private static final long serialVersionUID = 1L;
}
