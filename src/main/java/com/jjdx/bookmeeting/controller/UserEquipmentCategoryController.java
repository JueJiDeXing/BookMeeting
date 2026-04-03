package com.jjdx.bookmeeting.controller;

import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.model.entity.EquipmentCategory;
import com.jjdx.bookmeeting.service.EquipmentCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 用户端-设备分类接口
 */
@RestController
@RequestMapping("/user/equipment/category")
@Slf4j
public class UserEquipmentCategoryController {

    @Resource
    private EquipmentCategoryService equipmentCategoryService;

    /**
     获取所有设备分类（用于用户筛选）
     */
    @GetMapping("/list/all")
    public BaseResponse<List<EquipmentCategory>> listAllCategories(HttpServletRequest request) {
        List<EquipmentCategory> categoryList = equipmentCategoryService.lambdaQuery()
                .orderByAsc(EquipmentCategory::getSortOrder)
                .orderByAsc(EquipmentCategory::getId)
                .list();
        return ResultUtils.success(categoryList);
    }
}
