package com.jjdx.bookmeeting.model.dto.admin.equipment.category;

import com.jjdx.bookmeeting.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 查询设备分类请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EquipmentCategoryQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     分类名称（模糊查询）
     */
    private String categoryName;
}
