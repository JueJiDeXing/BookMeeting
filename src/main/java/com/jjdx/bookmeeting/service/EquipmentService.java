// EquipmentService.java
package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.dto.admin.equipment.EquipmentQueryRequest;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.vo.EquipmentVO;

import java.util.List;

/**
 设备服务
 */
public interface EquipmentService extends IService<Equipment> {


    /**
     更新设备
     */
    boolean updateEquipment(Long id, String equipmentName, String equipmentCode, Long categoryId, String icon, Integer status, Long operatorId);

    /**
     删除设备（逻辑删除）
     */
    boolean deleteEquipment(Long id);

    /**
     批量删除设备
     */
    boolean deleteEquipmentBatch(List<Long> ids);

    /**
     获取设备VO（带使用情况）
     */
    EquipmentVO getEquipmentVO(Equipment equipment);

    /**
     获取设备列表（带使用情况）
     */
    List<EquipmentVO> getEquipmentVOList(List<Equipment> equipmentList);

    /**
     检查设备是否正在被会议室使用
     */
    boolean isEquipmentInUse(Long equipmentId);

    /**
     获取设备使用统计
     */
    int countEquipmentUsage(Long equipmentId);

    /**
     根据状态查询设备
     */
    List<Equipment> getEquipmentByStatus(Integer status);

    /**
     查询可用的设备
     */
    List<Equipment> getAvailableEquipment();
    // 在 EquipmentService 接口中添加

    /**
     添加设备
     */
    Equipment addEquipment(String equipmentName, String equipmentCode, Long categoryId, String icon, Integer status, Long operatorId);


    /**
     更新设备状态（带日志）
     */
    void updateEquipmentStatus(Long id, Integer status, String remark, Long operatorId);

    /**
     设备移入会议室
     */
    boolean moveEquipmentIn(Long equipmentId, Long roomId, String remark, Long operatorId);

    /**
     设备移出会议室
     */
    boolean moveEquipmentOut(Long equipmentId, String remark, Long operatorId);

    /**
     设备报废
     */
    boolean scrapEquipment(Long equipmentId, String remark, Long operatorId);

    /**
     获取查询包装器
     */
    QueryWrapper<Equipment> getQueryWrapper(EquipmentQueryRequest queryRequest);
}
