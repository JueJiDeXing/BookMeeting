package com.jjdx.bookmeeting.model.dto.admin.equipment;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 设备批量删除请求
 */
@Data
public class EquipmentBatchDeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     设备ID列表
     */
    private List<Long> ids;
}
