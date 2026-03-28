package com.jjdx.bookmeeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备表实体类
 */
@Data
@TableName("equipment")
public class Equipment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备名称，如：白板、投影仪
     */
    private String equipmentName;

    /**
     * 设备代码
     */
    private String equipmentCode;

    /**
     * 状态（0-正常 1-不可用）
     */
    private Integer status;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer isDelete;

    private Long categoryId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
