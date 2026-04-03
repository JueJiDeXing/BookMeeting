package com.jjdx.bookmeeting.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.bookmeeting.common.ErrorCode;
import com.jjdx.bookmeeting.exception.BusinessException;
import com.jjdx.bookmeeting.mapper.EquipmentCategoryMapper;
import com.jjdx.bookmeeting.model.entity.Equipment;
import com.jjdx.bookmeeting.model.entity.EquipmentCategory;
import com.jjdx.bookmeeting.service.EquipmentCategoryService;
import com.jjdx.bookmeeting.service.EquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 设备分类服务实现
 */
@Service
@Slf4j
public class EquipmentCategoryServiceImpl extends ServiceImpl<EquipmentCategoryMapper, EquipmentCategory>
        implements EquipmentCategoryService {

    @Resource
    private EquipmentService equipmentService;

    @Override
    public List<EquipmentCategory> getAllCategories() {
        return lambdaQuery()
                .orderByAsc(EquipmentCategory::getSortOrder)
                .orderByAsc(EquipmentCategory::getId)
                .list();
    }

    @Override
    public EquipmentCategory getByCategoryName(String categoryName) {
        return lambdaQuery()
                .eq(EquipmentCategory::getCategoryName, categoryName)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddCategories(List<String> categoryNames) {
        if (CollectionUtils.isEmpty(categoryNames)) {
            return true;
        }

        // 去重
        List<String> distinctNames = categoryNames.stream()
                .distinct()
                .collect(Collectors.toList());

        // 过滤掉已存在的分类
        List<String> existingNames = lambdaQuery()
                .in(EquipmentCategory::getCategoryName, distinctNames)
                .list()
                .stream()
                .map(EquipmentCategory::getCategoryName)
                .collect(Collectors.toList());

        List<String> newNames = distinctNames.stream()
                .filter(name -> !existingNames.contains(name))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(newNames)) {
            return true;
        }

        // 获取最大排序值
        Integer maxSort = lambdaQuery()
                .orderByDesc(EquipmentCategory::getSortOrder)
                .last("LIMIT 1")
                .one()
                .getSortOrder();

        AtomicInteger startSort = new AtomicInteger((maxSort == null ? 0 : maxSort) + 1);

        // 创建新分类
        List<EquipmentCategory> categoryList = newNames.stream()
                .map(name -> {
                    EquipmentCategory category = new EquipmentCategory();
                    category.setCategoryName(name);
                    category.setSortOrder(startSort.getAndIncrement());
                    return category;
                })
                .collect(Collectors.toList());

        boolean saved = saveBatch(categoryList);
        if (saved) {
            log.info("批量新增设备分类: {}", newNames);
        }
        return saved;
    }

    @Override
    public boolean isCategoryInUse(Long categoryId) {
        // 查询该分类下是否有设备
        Long count = equipmentService.lambdaQuery()
                .eq(Equipment::getCategoryId, categoryId)
                .eq(Equipment::getIsDelete, 0)
                .count();
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategoryIfNotUsed(Long categoryId) {
        // 检查分类是否存在
        EquipmentCategory category = getById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在");
        }

        // 检查是否被使用
        if (isCategoryInUse(categoryId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "分类【" + category.getCategoryName() + "】下存在设备，无法删除");
        }

        boolean removed = removeById(categoryId);
        if (removed) {
            log.info("删除设备分类: {}", category.getCategoryName());
        }
        return removed;
    }
}
