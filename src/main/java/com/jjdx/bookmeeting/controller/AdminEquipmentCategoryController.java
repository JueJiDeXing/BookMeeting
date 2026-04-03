package com.jjdx.bookmeeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.bookmeeting.common.BaseResponse;
import com.jjdx.bookmeeting.common.DeleteRequest;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.common.ResultUtils;
import com.jjdx.bookmeeting.constant.UserConstant;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.exception.ThrowUtils;
import com.jjdx.bookmeeting.interceptor.aop.annotation.AuthCheck;
import com.jjdx.bookmeeting.model.dto.admin.equipment.category.*;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.entity.EquipmentCategory;
import com.jjdx.bookmeeting.model.vo.EquipmentCategoryVO;
import com.jjdx.bookmeeting.service.EquipmentCategoryService;
import com.jjdx.bookmeeting.service.EquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 管理员端-设备分类管理接口
 */
@RestController
@RequestMapping("/admin/equipment/category")
@Slf4j
public class AdminEquipmentCategoryController {

    @Resource
    private EquipmentCategoryService equipmentCategoryService;

    @Resource
    private EquipmentService equipmentService;

    /**
     新增分类
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addCategory(@RequestBody EquipmentCategoryAddRequest addRequest,
                                          HttpServletRequest request) {
        if (addRequest == null || addRequest.getCategoryName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查分类名是否已存在
        EquipmentCategory existing = equipmentCategoryService.getByCategoryName(addRequest.getCategoryName());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称已存在");
        }

        EquipmentCategory category = new EquipmentCategory();
        BeanUtils.copyProperties(addRequest, category);

        if (!equipmentCategoryService.save(category)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增失败");
        }

        return ResultUtils.success(category.getId());
    }

    /**
     批量新增分类
     */
    @PostMapping("/add/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchAddCategory(@RequestBody EquipmentCategoryBatchAddRequest batchRequest,
                                                  HttpServletRequest request) {
        if (batchRequest == null || batchRequest.getCategoryNames() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = equipmentCategoryService.batchAddCategories(batchRequest.getCategoryNames());
        return ResultUtils.success(result);
    }

    /**
     删除分类
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteCategory(@RequestBody DeleteRequest deleteRequest,
                                                HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = equipmentCategoryService.deleteCategoryIfNotUsed(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     批量删除分类
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteCategory(@RequestBody EquipmentCategoryBatchDeleteRequest batchDeleteRequest,
                                                     HttpServletRequest request) {
        if (batchDeleteRequest == null || batchDeleteRequest.getIds() == null || batchDeleteRequest.getIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean allSuccess = true;
        for (Long id : batchDeleteRequest.getIds()) {
            try {
                equipmentCategoryService.deleteCategoryIfNotUsed(id);
            } catch (Exception e) {
                log.error("批量删除分类失败，id: {}", id, e);
                allSuccess = false;
            }
        }

        return ResultUtils.success(allSuccess);
    }

    /**
     更新分类
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateCategory(@RequestBody EquipmentCategoryUpdateRequest updateRequest,
                                                HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        EquipmentCategory category = equipmentCategoryService.getById(updateRequest.getId());
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在");
        }

        // 如果修改了分类名，检查是否与其他分类冲突
        if (updateRequest.getCategoryName() != null &&
                !updateRequest.getCategoryName().equals(category.getCategoryName())) {
            EquipmentCategory existing = equipmentCategoryService.getByCategoryName(updateRequest.getCategoryName());
            if (existing != null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称已存在");
            }
            category.setCategoryName(updateRequest.getCategoryName());
        }

        if (updateRequest.getSortOrder() != null) {
            category.setSortOrder(updateRequest.getSortOrder());
        }

        boolean updated = equipmentCategoryService.updateById(category);
        return ResultUtils.success(updated);
    }

    /**
     分页获取分类列表（带设备数量）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<EquipmentCategoryVO>> listCategoryByPage(@RequestBody EquipmentCategoryQueryRequest queryRequest,
                                                                      HttpServletRequest request) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();

        // 查询分类列表
        Page<EquipmentCategory> page = equipmentCategoryService.page(
                new Page<>(current, size),
                equipmentCategoryService.lambdaQuery()
                        .like(queryRequest.getCategoryName() != null,
                                EquipmentCategory::getCategoryName, queryRequest.getCategoryName())
                        .orderByAsc(EquipmentCategory::getSortOrder)
                        .orderByAsc(EquipmentCategory::getId)
                        .getWrapper()
        );

        // 转换为VO，并统计每个分类下的设备数量
        Page<EquipmentCategoryVO> voPage = new Page<>(current, size, page.getTotal());
        List<EquipmentCategoryVO> voList = page.getRecords().stream()
                .map(category -> {
                    EquipmentCategoryVO vo = new EquipmentCategoryVO();
                    BeanUtils.copyProperties(category, vo);

                    // 统计该分类下的设备数量
                    Long count = equipmentService.lambdaQuery()
                            .eq(Equipment::getCategoryId, category.getId())
                            .eq(Equipment::getIsDelete, 0)
                            .count();
                    vo.setEquipmentCount(count.intValue());

                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     获取所有分类（不分页）
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<EquipmentCategory>> listAllCategories(HttpServletRequest request) {
        List<EquipmentCategory> list = equipmentCategoryService.lambdaQuery()
                .orderByAsc(EquipmentCategory::getSortOrder)
                .orderByAsc(EquipmentCategory::getId)
                .list();
        return ResultUtils.success(list);
    }

    /**
     根据ID获取分类详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<EquipmentCategory> getCategoryById(@RequestParam long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        EquipmentCategory category = equipmentCategoryService.getById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(category);
    }
}
