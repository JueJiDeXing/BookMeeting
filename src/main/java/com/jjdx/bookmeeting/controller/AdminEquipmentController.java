package com.jjdx.bookmeeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.DeleteRequest;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.constant.UserConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.interceptor.aop.annotation.AuthCheck;
import com.jjdx.bookmeeting.model.dto.admin.equipment.*;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.vo.EquipmentVO;
import com.jjdx.bookmeeting.service.EquipmentService;
import com.jjdx.bookmeeting.service.MeetingRoomService;
import com.jjdx.bookmeeting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 管理员-设备管理接口
 */
@RestController
@RequestMapping("/admin/equipment")
@Slf4j
public class AdminEquipmentController {

    @Resource
    private EquipmentService equipmentService;

    @Resource
    private UserService userService;

    @Resource
    private MeetingRoomService meetingRoomService;

    // region 增删改查


    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addEquipment(@RequestBody EquipmentAddRequest addRequest,
                                           HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验参数
        validateEquipmentAddRequest(addRequest);

        // 检查设备代码是否已存在
        Equipment existingEquipment = equipmentService.lambdaQuery()
                .eq(Equipment::getEquipmentCode, addRequest.getEquipmentCode())
                .one();
        if (existingEquipment != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码已存在");
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        Equipment equipment = equipmentService.addEquipment(
                addRequest.getEquipmentName(),
                addRequest.getEquipmentCode(),
                addRequest.getCategoryId(),
                addRequest.getIcon(),
                addRequest.getStatus(),
                operatorId
        );

        return ResultUtils.success(equipment.getId());
    }
    /**
     删除设备
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteEquipment(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查设备是否存在
        Equipment equipment = equipmentService.getById(deleteRequest.getId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 检查设备是否正在被使用
        if (equipmentService.isEquipmentInUse(deleteRequest.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "设备【" + equipment.getEquipmentName() + "】正在被会议室使用，无法删除");
        }

        boolean removed = equipmentService.deleteEquipment(deleteRequest.getId());
        return ResultUtils.success(removed);
    }

    /**
     批量删除设备
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteEquipment(@RequestBody EquipmentBatchDeleteRequest batchDeleteRequest,
                                                      HttpServletRequest request) {
        if (batchDeleteRequest == null || batchDeleteRequest.getIds() == null || batchDeleteRequest.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean removed = equipmentService.deleteEquipmentBatch(batchDeleteRequest.getIds());
        return ResultUtils.success(removed);
    }


    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateEquipment(@RequestBody EquipmentUpdateRequest updateRequest,
                                                 HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查设备是否存在
        Equipment oldEquipment = equipmentService.getById(updateRequest.getId());
        if (oldEquipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 校验参数
        validateEquipmentUpdateRequest(updateRequest);

        // 检查设备代码是否与其他设备重复
        if (updateRequest.getEquipmentCode() != null &&
                !updateRequest.getEquipmentCode().equals(oldEquipment.getEquipmentCode())) {
            Equipment existingEquipment = equipmentService.lambdaQuery()
                    .eq(Equipment::getEquipmentCode, updateRequest.getEquipmentCode())
                    .ne(Equipment::getId, updateRequest.getId())
                    .one();
            if (existingEquipment != null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码已存在");
            }
        }
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean updated = equipmentService.updateEquipment(
                updateRequest.getId(),
                updateRequest.getEquipmentName(),
                updateRequest.getEquipmentCode(),
                updateRequest.getCategoryId(),  // ✅ 新增
                updateRequest.getIcon(),
                updateRequest.getStatus(),
                operatorId
        );

        return ResultUtils.success(updated);
    }
    /**
     更新设备状态
     */
    @PostMapping("/update/status")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateEquipmentStatus(@RequestBody EquipmentUpdateStatusRequest statusRequest,
                                                       HttpServletRequest request) {
        if (statusRequest == null || statusRequest.getId() == null || statusRequest.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查设备是否存在
        Equipment equipment = equipmentService.getById(statusRequest.getId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 校验状态值
        if (statusRequest.getStatus() < 0 || statusRequest.getStatus() > 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效");
        }

        // 如果要将状态改为不可用(1)，需要检查设备是否正在使用
        if (statusRequest.getStatus() == 1 && equipmentService.isEquipmentInUse(statusRequest.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备正在被使用，无法设为不可用");
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        // 记录日志并更新状态
        equipmentService.updateEquipmentStatus(
                statusRequest.getId(),
                statusRequest.getStatus(),
                statusRequest.getRemark(),
                operatorId
        );

        return ResultUtils.success(true);
    }

    /**
     设备移入会议室
     */
    @PostMapping("/move/in")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> moveEquipmentIn(@RequestBody EquipmentMoveRequest moveRequest,
                                                 HttpServletRequest request) {
        if (moveRequest == null || moveRequest.getId() == null || moveRequest.getTargetRoomId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查设备是否存在
        Equipment equipment = equipmentService.getById(moveRequest.getId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 检查设备状态是否正常
        if (equipment.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备状态异常，无法移入");
        }

        // 检查设备是否已在其他会议室
        if (equipmentService.isEquipmentInUse(moveRequest.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备已在其他会议室，请先移出");
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean moved = equipmentService.moveEquipmentIn(
                moveRequest.getId(),
                moveRequest.getTargetRoomId(),
                moveRequest.getRemark(),
                operatorId
        );

        return ResultUtils.success(moved);
    }

    /**
     设备移出会议室
     */
    @PostMapping("/move/out")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> moveEquipmentOut(@RequestBody EquipmentMoveRequest moveRequest,
                                                  HttpServletRequest request) {
        if (moveRequest == null || moveRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查设备是否存在
        Equipment equipment = equipmentService.getById(moveRequest.getId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 检查设备是否正在使用
        if (!equipmentService.isEquipmentInUse(moveRequest.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备未在任何会议室中");
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean moved = equipmentService.moveEquipmentOut(
                moveRequest.getId(),
                moveRequest.getRemark(),
                operatorId
        );

        return ResultUtils.success(moved);
    }

    /**
     设备报废
     */
    @PostMapping("/scrap")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> scrapEquipment(@RequestBody EquipmentMoveRequest moveRequest,
                                                HttpServletRequest request) {
        if (moveRequest == null || moveRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查设备是否存在
        Equipment equipment = equipmentService.getById(moveRequest.getId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        }

        // 获取当前操作人ID
        Long operatorId = Long.parseLong(request.getAttribute("id").toString());

        boolean scrapped = equipmentService.scrapEquipment(
                moveRequest.getId(),
                moveRequest.getRemark(),
                operatorId
        );

        return ResultUtils.success(scrapped);
    }

    /**
     根据 id 获取设备
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Equipment> getEquipmentById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Equipment equipment = equipmentService.getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(equipment);
    }

    /**
     根据 id 获取设备VO（带使用信息）
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<EquipmentVO> getEquipmentVOById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Equipment equipment = equipmentService.getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        EquipmentVO equipmentVO = equipmentService.getEquipmentVO(equipment);
        return ResultUtils.success(equipmentVO);
    }

    /**
     分页获取设备列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Equipment>> listEquipmentByPage(@RequestBody EquipmentQueryRequest queryRequest,
                                                             HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        // 限制爬虫
        

        Page<Equipment> equipmentPage = equipmentService.page(
                new Page<>(current, size),
                equipmentService.getQueryWrapper(queryRequest)
        );

        return ResultUtils.success(equipmentPage);
    }

    /**
     分页获取设备VO列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<EquipmentVO>> listEquipmentVOByPage(@RequestBody EquipmentQueryRequest queryRequest,
                                                                 HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        Page<Equipment> equipmentPage = equipmentService.page(
                new Page<>(current, size),
                equipmentService.getQueryWrapper(queryRequest)
        );

        Page<EquipmentVO> equipmentVOPage = new Page<>(current, size, equipmentPage.getTotal());
        List<EquipmentVO> equipmentVOList = equipmentPage.getRecords().stream()
                .map(equipment -> equipmentService.getEquipmentVO(equipment))
                .collect(Collectors.toList());
        equipmentVOPage.setRecords(equipmentVOList);

        return ResultUtils.success(equipmentVOPage);
    }

    /**
     获取所有可用设备列表（用于下拉框）
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Equipment>> listAllAvailableEquipment(HttpServletRequest request) {
        List<Equipment> equipmentList = equipmentService.getAvailableEquipment();
        return ResultUtils.success(equipmentList);
    }

    // endregion


    // region 参数校验

    /**
     校验设备创建请求
     */
    private void validateEquipmentAddRequest(EquipmentAddRequest request) {
        if (request.getEquipmentName() == null || request.getEquipmentName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备名称不能为空");
        }
        if (request.getEquipmentName().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备名称不能超过50个字符");
        }
        if (request.getEquipmentCode() == null || request.getEquipmentCode().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码不能为空");
        }
        if (request.getEquipmentCode().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码不能超过50个字符");
        }
        if (request.getStatus() != null && (request.getStatus() < 0 || request.getStatus() > 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效");
        }
    }

    /**
     校验设备更新请求
     */
    private void validateEquipmentUpdateRequest(EquipmentUpdateRequest request) {
        if (request.getEquipmentName() != null) {
            if (request.getEquipmentName().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备名称不能为空");
            }
            if (request.getEquipmentName().length() > 50) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备名称不能超过50个字符");
            }
        }
        if (request.getEquipmentCode() != null) {
            if (request.getEquipmentCode().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码不能为空");
            }
            if (request.getEquipmentCode().length() > 50) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备代码不能超过50个字符");
            }
        }
        if (request.getStatus() != null && (request.getStatus() < 0 || request.getStatus() > 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效");
        }
        if (request.getCategoryId() != null && request.getCategoryId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID无效");
        }
    }

    // endregion
}
