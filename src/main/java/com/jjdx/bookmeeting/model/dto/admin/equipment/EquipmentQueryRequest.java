package com.jjdx.bookmeeting.model.dto.admin.equipment;

import com.jjdx.bookmeeting.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 设备查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EquipmentQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     设备ID
     */
    private Long id;

    /**
     设备名称（模糊查询）
     */
    private String equipmentName;

    /**
     设备代码
     */
    private String equipmentCode;

    /**
     分类ID ✅ 新增
     */
    private Long categoryId;

    /**
     状态（0-正常 1-不可用）
     */
    private Integer status;

    /**
     所在会议室ID
     */
    private Long roomId;

    /**
     是否正在使用
     */
    private Boolean inUse;

    /**
     是否删除
     */
    private Integer isDelete;
}
