package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.enums.EquipmentStatusEnum;
import com.jjdx.bookmeeting.service.EquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 用户端-设备接口
 */
@RestController
@RequestMapping("/user/equipment")
@Slf4j
public class UserEquipmentController {

    @Resource
    private EquipmentService equipmentService;

    /**
     获取所有可用设备列表
     */
    @GetMapping("/list/all")
    public BaseResponse<List<Equipment>> listAllAvailableEquipment(HttpServletRequest request) {
        List<Equipment> equipmentList = equipmentService.lambdaQuery()
                .eq(Equipment::getStatus, EquipmentStatusEnum.NORMAL)
                .eq(Equipment::getIsDelete, 0)
                .orderByAsc(Equipment::getEquipmentName)
                .list();
        return ResultUtils.success(equipmentList);
    }
}
