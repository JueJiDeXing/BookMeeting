package com.jjdx.bookmeeting.model.vo;

import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备视图对象（带使用情况）
 */
@Data
public class EquipmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private Long id;

    /**
     * 设备名称
     */
    private String equipmentName;

    /**
     * 设备代码
     */
    private String equipmentCode;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称 ✅ 新增
     */
    private String categoryName;

    /**
     * 状态（0-正常 1-不可用）
     */
    private Integer status;

    /**
     * 是否正在使用
     */
    private Boolean inUse;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 当前所在的会议室
     */
    private MeetingRoom currentRoom;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
