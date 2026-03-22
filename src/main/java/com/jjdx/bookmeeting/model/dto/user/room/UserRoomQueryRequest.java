package com.jjdx.bookmeeting.model.dto.user.room;

import com.jjdx.bookmeeting.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端-会议室查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserRoomQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会议室名称（模糊查询）
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
     * 最小容纳人数
     */
    private Integer minCapacity;

    /**
     * 最大容纳人数
     */
    private Integer maxCapacity;

    /**
     * 会议室状态
     */
    private Integer status;

    /**
     * 设备分类ID列表（用户按分类筛选）
     */
    private List<Long> categoryIds;

    /**
     * 是否删除
     */
    private Integer isDelete;
}
