package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.constant.CommonConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.EquipmentLogMapper;
import com.jjdx.bookmeeting.model.dto.admin.equipment.EquipmentLogQueryRequest;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.entity.EquipmentLog;
import com.jjdx.bookmeeting.service.EquipmentLogService;
import com.jjdx.bookmeeting.service.EquipmentService;
import com.jjdx.bookmeeting.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 设备日志服务实现
 */
@Service
@Slf4j
public class EquipmentLogServiceImpl extends ServiceImpl<EquipmentLogMapper, EquipmentLog> implements EquipmentLogService {

    @Resource
    @Lazy
    private EquipmentService equipmentService;

    @Override
    public void logOperation(Long equipmentId, Long operatorId, Long fromRoomId, Long toRoomId,
                             Integer operationType, Integer oldStatus, Integer newStatus, String remark) {
        EquipmentLog log = new EquipmentLog();
        log.setEquipmentId(equipmentId);
        log.setOperatorId(operatorId);
        log.setFromRoomId(fromRoomId);
        log.setToRoomId(toRoomId);
        log.setOperationType(operationType);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setRemark(remark);

        save(log);
    }

    @Override
    public QueryWrapper<EquipmentLog> getQueryWrapper(EquipmentLogQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long equipmentId = queryRequest.getEquipmentId();
        String equipmentName = queryRequest.getEquipmentName();
        Long operatorId = queryRequest.getOperatorId();
        Integer operationType = queryRequest.getOperationType();
        LocalDateTime startTime = queryRequest.getStartTime();
        LocalDateTime endTime = queryRequest.getEndTime();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        QueryWrapper<EquipmentLog> queryWrapper = new QueryWrapper<>();

        // 精确匹配
        queryWrapper.eq(equipmentId != null, "equipment_id", equipmentId);
        queryWrapper.eq(operatorId != null, "operator_id", operatorId);
        queryWrapper.eq(operationType != null, "operation_type", operationType);

        // 如果有设备名称，需要通过联查 equipment 表获取设备ID
        if (StringUtils.isNotBlank(equipmentName)) {
            // 先查询设备表获取匹配的设备ID
            QueryWrapper<Equipment> equipmentWrapper = new QueryWrapper<>();
            equipmentWrapper.like("equipment_name", equipmentName);
            List<Equipment> equipmentList = equipmentService.list(equipmentWrapper);
            if (!equipmentList.isEmpty()) {
                List<Long> equipmentIds = equipmentList.stream()
                        .map(Equipment::getId)
                        .collect(Collectors.toList());
                queryWrapper.in("equipment_id", equipmentIds);
            } else {
                // 没有匹配的设备，返回空结果
                queryWrapper.eq("id", 0);
            }
        }

        // 时间范围查询
        if (startTime != null && endTime != null) {
            queryWrapper.between("create_time", startTime, endTime);
        } else if (startTime != null) {
            queryWrapper.ge("create_time", startTime);
        } else if (endTime != null) {
            queryWrapper.le("create_time", endTime);
        }

        // 排序
        if (SqlUtils.validSortField(sortField)) {
            queryWrapper.orderBy(true, CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        } else {
            // 默认按创建时间倒序
            queryWrapper.orderByDesc("create_time");
        }

        return queryWrapper;
    }
}
