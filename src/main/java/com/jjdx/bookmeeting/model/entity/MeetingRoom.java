package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会议室表实体类
 */
@Data
@TableName("meeting_room")
public class MeetingRoom implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会议室ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会议室名称
     */
    private String roomName;

    /**
     * 楼栋
     */
    private String building;

    /**
     * 楼层
     */
    private Integer floor;

    /**
     * 房间号
     */
    private String roomNumber;

    /**
     * 完整位置描述（自动生成）
     */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String locationDesc;

    /**
     * 可容纳人数
     */
    private Integer capacity;

    /**
     * 会议室描述
     */
    private String description;

    /**
     * 会议室图片URL
     */
    private String imageUrl;

    /**
     * 状态（0-可用 1-维护中 2-被占用）
     */
    private Integer status;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
