package com.jjdx.bookmeeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jjdx.bookmeeting.model.entity.EquipmentCategory;

import java.util.List;

/**
 * 设备分类服务接口
 */
public interface EquipmentCategoryService extends IService<EquipmentCategory> {

    /**
     * 获取所有分类（按排序字段升序）
     */
    List<EquipmentCategory> getAllCategories();

    /**
     * 根据分类名称查询
     */
    EquipmentCategory getByCategoryName(String categoryName);

    /**
     * 批量新增分类
     */
    boolean batchAddCategories(List<String> categoryNames);

    /**
     * 检查分类是否被设备使用
     */
    boolean isCategoryInUse(Long categoryId);

    /**
     * 删除分类（如果未被使用）
     */
    boolean deleteCategoryIfNotUsed(Long categoryId);
}
