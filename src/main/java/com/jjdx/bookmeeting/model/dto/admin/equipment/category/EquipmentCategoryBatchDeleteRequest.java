package com.jjdx.bookmeeting.model.dto.admin.equipment.category;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量删除设备分类请求
 */
@Data
public class EquipmentCategoryBatchDeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID列表
     */
    private List<Long> ids;
}
