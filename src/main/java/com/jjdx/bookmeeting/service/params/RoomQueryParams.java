package com.jjdx.bookmeeting.service.params;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 会议室查询参数
 */
@Data
public class RoomQueryParams implements Serializable {

    /**
     会议室ID
     */
    private Long id;

    /**
     会议室名称（模糊查询）
     */
    private String roomName;

    /**
     楼栋（精确匹配）
     */
    private String building;

    /**
     楼层（精确匹配）
     */
    private Integer floor;

    /**
     最小容纳人数
     */
    private Integer minCapacity;

    /**
     最大容纳人数
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

    /**
     设备ID（查询包含某设备的会议室）
     */
    private Long equipmentId;

    /**
     设备分类ID列表（用户按分类筛选）
     */
    private List<Long> categoryIds;

    /**
     排序字段
     */
    private String sortField;

    /**
     排序顺序（asc/desc）
     */
    private String sortOrder;

    private static final long serialVersionUID = 1L;
}
