package com.jjdx.bookmeeting.model.dto.admin.equipment.category;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 批量新增设备分类请求
 */
@Data
public class EquipmentCategoryBatchAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     分类名称列表
     */
    private List<String> categoryNames;
}
