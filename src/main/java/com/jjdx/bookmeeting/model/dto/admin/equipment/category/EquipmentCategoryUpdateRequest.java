package com.jjdx.bookmeeting.model.dto.admin.equipment.category;

import lombok.Data;

import java.io.Serializable;

/**
 更新设备分类请求
 */
@Data
public class EquipmentCategoryUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     分类ID
     */
    private Long id;

    /**
     分类名称
     */
    private String categoryName;

    /**
     排序
     */
    private Integer sortOrder;
}
