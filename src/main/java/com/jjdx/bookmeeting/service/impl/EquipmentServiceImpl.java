package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.constant.CommonConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.EquipmentMapper;
import com.jjdx.bookmeeting.model.dto.admin.equipment.EquipmentQueryRequest;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.entity.EquipmentCategory;
import com.jjdx.bookmeeting.model.entity.MeetingRoom;
import com.jjdx.bookmeeting.model.entity.RoomEquipment;
import com.jjdx.bookmeeting.model.vo.EquipmentVO;
import com.jjdx.bookmeeting.service.*;
import com.jjdx.bookmeeting.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 设备服务实现
 */
@Service
@Slf4j
public class EquipmentServiceImpl extends ServiceImpl<EquipmentMapper, Equipment> implements EquipmentService {

    @Resource
    private RoomEquipmentService roomEquipmentService;

    @Resource
    @Lazy
    private MeetingRoomService meetingRoomService;

    @Resource
    @Lazy
    private EquipmentCategoryService equipmentCategoryService;

    // addEquipment 方法增加 categoryId 参数
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Equipment addEquipment(String equipmentName, String equipmentCode, Long categoryId, Integer status, Long operatorId) {
        // 校验参数
        if (StringUtils.isBlank(equipmentName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备名称不能为空");
        }
        if (StringUtils.isBlank(equipmentCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码不能为空");
        }
        if (categoryId == null) {  // ✅ 新增校验
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择设备分类");
        }

        // 检查分类是否存在
        EquipmentCategory category = equipmentCategoryService.getById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备分类不存在");
        }

        // 检查设备代码是否已存在
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Equipment::getEquipmentCode, equipmentCode);
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码已存在");
        }

        // 创建设备
        Equipment equipment = new Equipment();
        equipment.setEquipmentName(equipmentName);
        equipment.setEquipmentCode(equipmentCode);
        equipment.setCategoryId(categoryId);  // ✅ 新增
        equipment.setStatus(status != null ? status : 0);

        boolean saved = save(equipment);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加设备失败");
        }

        log.info("管理员[{}]新增设备[{}]，分类：{}", operatorId, equipment.getEquipmentName(), category.getCategoryName());
        return equipment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEquipment(Long id, String equipmentName, String equipmentCode, Long categoryId, Integer status, Long operatorId) {
        // 检查设备是否存在
        Equipment equipment = getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 如果修改了分类，检查分类是否存在
        if (categoryId != null && !categoryId.equals(equipment.getCategoryId())) {
            EquipmentCategory category = equipmentCategoryService.getById(categoryId);
            if (category == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备分类不存在");
            }
        }

        // 检查设备代码是否与其他设备重复
        if (StringUtils.isNotBlank(equipmentCode) && !equipmentCode.equals(equipment.getEquipmentCode())) {
            LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Equipment::getEquipmentCode, equipmentCode)
                    .ne(Equipment::getId, id);
            if (count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码已存在");
            }
        }

        // 记录旧状态
        Integer oldStatus = equipment.getStatus();

        // 更新设备
        Equipment updateEquipment = new Equipment();
        updateEquipment.setId(id);
        updateEquipment.setEquipmentName(equipmentName);
        updateEquipment.setEquipmentCode(equipmentCode);
        updateEquipment.setCategoryId(categoryId);  // ✅ 新增
        updateEquipment.setStatus(status);

        return updateById(updateEquipment);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentStatus(Long id, Integer status, String remark, Long operatorId) {
        // 检查设备是否存在
        Equipment equipment = getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 如果状态没有变化，直接返回
        if (equipment.getStatus().equals(status)) {
            return;
        }

        Integer oldStatus = equipment.getStatus();

        // 更新状态
        equipment.setStatus(status);
        updateById(equipment);

        log.info("管理员[{}]更新设备[{}]状态: {} -> {}, 备注: {}",
                operatorId, equipment.getEquipmentName(), oldStatus, status, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveEquipmentIn(Long equipmentId, Long roomId, String remark, Long operatorId) {
        // 检查设备是否存在
        Equipment equipment = getById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 检查设备状态是否正常
        if (equipment.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备状态异常，无法移入");
        }

        // 检查设备是否已在其他会议室
        if (isEquipmentInUse(equipmentId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备已在其他会议室，请先移出");
        }

        // 检查目标会议室是否存在
        MeetingRoom room = meetingRoomService.getById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "目标会议室不存在");
        }

        // 创建会议室-设备关联
        RoomEquipment roomEquipment = new RoomEquipment();
        roomEquipment.setRoomId(roomId);
        roomEquipment.setEquipmentId(equipmentId);
        roomEquipment.setIsAvailable(1);
        roomEquipmentService.save(roomEquipment);

        log.info("管理员[{}]将设备[{}]移入会议室[{}]", operatorId, equipment.getEquipmentName(), room.getRoomName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveEquipmentOut(Long equipmentId, String remark, Long operatorId) {
        // 检查设备是否存在
        Equipment equipment = getById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 获取当前所在的会议室
        LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomEquipment::getEquipmentId, equipmentId);
        RoomEquipment roomEquipment = roomEquipmentService.getOne(wrapper);

        if (roomEquipment == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备未在任何会议室中");
        }

        Long fromRoomId = roomEquipment.getRoomId();
        MeetingRoom fromRoom = meetingRoomService.getById(fromRoomId);

        // 删除关联
        roomEquipmentService.removeById(roomEquipment.getId());

        log.info("管理员[{}]将设备[{}]从会议室[{}]移出",
                operatorId, equipment.getEquipmentName(), fromRoom != null ? fromRoom.getRoomName() : "未知");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean scrapEquipment(Long equipmentId, String remark, Long operatorId) {
        // 检查设备是否存在
        Equipment equipment = getById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 如果设备在会议室中，需要先移出
        if (isEquipmentInUse(equipmentId)) {
            // 获取当前所在的会议室
            LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RoomEquipment::getEquipmentId, equipmentId);
            RoomEquipment roomEquipment = roomEquipmentService.getOne(wrapper);

            if (roomEquipment != null) {
                // 删除关联
                roomEquipmentService.removeById(roomEquipment.getId());
            }
        }

        // 更新设备状态为不可用
        equipment.setStatus(1);
        updateById(equipment);

        log.info("管理员[{}]报废设备[{}]", operatorId, equipment.getEquipmentName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEquipment(Long id) {
        // 检查设备是否存在
        Equipment equipment = getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 检查设备是否正在被使用
        if (isEquipmentInUse(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "设备【" + equipment.getEquipmentName() + "】正在被会议室使用，无法删除");
        }

        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEquipmentBatch(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        // 检查设备是否正在被使用
        for (Long id : ids) {
            if (isEquipmentInUse(id)) {
                Equipment equipment = getById(id);
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "设备【" + equipment.getEquipmentName() + "】正在被使用，无法删除");
            }
        }

        return removeByIds(ids);
    }

    @Override
    public EquipmentVO getEquipmentVO(Equipment equipment) {
        if (equipment == null) {
            return null;
        }

        EquipmentVO equipmentVO = new EquipmentVO();
        BeanUtils.copyProperties(equipment, equipmentVO);

        // ✅ 获取设备分类名称
        if (equipment.getCategoryId() != null) {
            EquipmentCategory category = equipmentCategoryService.getById(equipment.getCategoryId());
            if (category != null) {
                equipmentVO.setCategoryName(category.getCategoryName());
            }
        }

        // 获取设备使用情况
        int usageCount = countEquipmentUsage(equipment.getId());
        equipmentVO.setUsageCount(usageCount);
        equipmentVO.setInUse(usageCount > 0);

        // 获取当前所在会议室
        if (usageCount > 0) {
            LambdaQueryWrapper<RoomEquipment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RoomEquipment::getEquipmentId, equipment.getId());
            RoomEquipment roomEquipment = roomEquipmentService.getOne(wrapper);
            if (roomEquipment != null) {
                MeetingRoom room = meetingRoomService.getById(roomEquipment.getRoomId());
                if (room != null) {
                    equipmentVO.setCurrentRoom(room);
                }
            }
        }

        return equipmentVO;
    }

    @Override
    public List<EquipmentVO> getEquipmentVOList(List<Equipment> equipmentList) {
        if (CollectionUtils.isEmpty(equipmentList)) {
            return new ArrayList<>();
        }

        return equipmentList.stream()
                .map(this::getEquipmentVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEquipmentInUse(Long equipmentId) {
        return countEquipmentUsage(equipmentId) > 0;
    }

    @Override
    public int countEquipmentUsage(Long equipmentId) {
        List<Long> roomIds = roomEquipmentService.getRoomIdsByEquipmentId(equipmentId);
        return roomIds.size();
    }

    @Override
    public List<Equipment> getEquipmentByStatus(Integer status) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Equipment::getStatus, status)
                .eq(Equipment::getIsDelete, 0)
                .orderByAsc(Equipment::getEquipmentName);

        return list(wrapper);
    }

    @Override
    public List<Equipment> getAvailableEquipment() {
        return getEquipmentByStatus(0);
    }

    @Override
    public QueryWrapper<Equipment> getQueryWrapper(EquipmentQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = queryRequest.getId();
        String equipmentName = queryRequest.getEquipmentName();
        String equipmentCode = queryRequest.getEquipmentCode();
        Long categoryId = queryRequest.getCategoryId();
        Integer status = queryRequest.getStatus();
        Long roomId = queryRequest.getRoomId();
        Boolean inUse = queryRequest.getInUse();
        Integer isDelete = queryRequest.getIsDelete();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        QueryWrapper<Equipment> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(equipmentCode), "equipment_code", equipmentCode);
        queryWrapper.eq(categoryId != null, "category_id", categoryId);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.eq(isDelete != null, "is_delete", isDelete);

        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(equipmentName), "equipment_name", equipmentName);

        // 根据所在会议室查询
        if (roomId != null || inUse != null) {
            // 查询设备ID列表
            LambdaQueryWrapper<RoomEquipment> reWrapper = new LambdaQueryWrapper<>();
            if (roomId != null) {
                reWrapper.eq(RoomEquipment::getRoomId, roomId);
            }

            List<RoomEquipment> reList = roomEquipmentService.list(reWrapper);
            if (CollectionUtils.isNotEmpty(reList)) {
                List<Long> equipmentIds = reList.stream()
                        .map(RoomEquipment::getEquipmentId)
                        .distinct()
                        .collect(Collectors.toList());
                if (inUse != null && inUse) {
                    // 正在使用
                    queryWrapper.in("id", equipmentIds);
                } else if (inUse != null) {
                    // 未使用
                    queryWrapper.notIn(CollectionUtils.isNotEmpty(equipmentIds), "id", equipmentIds);
                } else {
                    // 指定了roomId
                    queryWrapper.in("id", equipmentIds);
                }
            } else {
                // 没有找到任何关联，但要求查询正在使用的设备，则返回空结果
                if (inUse != null && inUse) {
                    queryWrapper.eq("id", 0); // 强制查不到结果
                }
            }
        }

        // 排序
        if (SqlUtils.validSortField(sortField)) {
            queryWrapper.orderBy(true, sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        } else {
            // 默认按创建时间倒序
            queryWrapper.orderByDesc("create_time");
        }

        return queryWrapper;
    }
}
