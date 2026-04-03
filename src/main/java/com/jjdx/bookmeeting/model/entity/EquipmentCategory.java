package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 设备分类实体类
 */
@Data
@TableName("equipment_category")
public class EquipmentCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     分类ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     分类名称，如：白板、投影、音频
     */
    private String categoryName;

    /**
     排序
     */
    private Integer sortOrder;

    /**
     创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
