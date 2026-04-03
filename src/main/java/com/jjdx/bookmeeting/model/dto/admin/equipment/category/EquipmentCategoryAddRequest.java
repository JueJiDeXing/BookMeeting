package com.jjdx.bookmeeting.model.dto.admin.equipment.category;

import lombok.Data;

import java.io.Serializable;

/**
 新增设备分类请求
 */
@Data
public class EquipmentCategoryAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     分类名称
     */
    private String categoryName;

    /**
     排序
     */
    private Integer sortOrder;
}
