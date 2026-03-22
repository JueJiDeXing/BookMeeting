package com.jjdx.bookmeeting.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备分类视图对象
 */
@Data
public class EquipmentCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 排序权重（数字越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 该分类下的设备数量
     */
    private Integer equipmentCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
