package com.jjdx.bookmeeting.model.dto.admin.room;

import com.jjdx.bookmeeting.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 会议室查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoomQueryRequest extends PageRequest implements Serializable {

    /**
     会议室ID
     */
    private Long id;

    /**
     会议室名称
     */
    private String roomName;

    /**
     楼栋
     */
    private String building;

    /**
     楼层
     */
    private Integer floor;

    /**
     容纳人数（最小）
     */
    private Integer minCapacity;

    /**
     容纳人数（最大）
     */
    private Integer maxCapacity;

    /**
     状态（0-可用 1-维护中 2-被占用）
     */
    private Integer status;

    /**
     是否删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
