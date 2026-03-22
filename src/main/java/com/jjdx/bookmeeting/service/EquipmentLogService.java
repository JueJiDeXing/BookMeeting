package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.dto.admin.equipment.EquipmentLogQueryRequest;
import com.jjdx.bookmeeting.model.entity.EquipmentLog;

/**
 * 设备日志服务
 */
public interface EquipmentLogService extends IService<EquipmentLog> {

    /**
     * 记录设备操作日志
     */
    void logOperation(Long equipmentId, Long operatorId, Long fromRoomId, Long toRoomId,
                     Integer operationType, Integer oldStatus, Integer newStatus, String remark);

    /**
     * 获取查询包装器
     */
    QueryWrapper<EquipmentLog> getQueryWrapper(EquipmentLogQueryRequest queryRequest);
}
