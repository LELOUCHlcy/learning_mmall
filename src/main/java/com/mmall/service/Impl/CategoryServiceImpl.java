package com.mmall.service.Impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Created by lcy on 2017/12/22.
 */
@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public ServerResponse addCategory(String categoryName, Integer parentId) {

        if (parentId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByError("添加品类时参数错误");
        }
        Category category = new Category();
        category.setParentId(parentId);
        category.setName(categoryName);
        category.setStatus(true);

        int rowCount = categoryMapper.insert(category);
        if (rowCount > 0) {
            return ServerResponse.createBySuccess("品类添加成功");
        }
        return ServerResponse.createByError("品类添加失败");
    }

    //更新品类名字
    @Override
    public ServerResponse setCategoryName(Integer categoryId, String categoryName) {
        if (categoryId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByError("更新品类名字参数错误");
        }
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);
        if (rowCount > 0) {
            return ServerResponse.createBySuccess("品类名称更新成功");
        }
        return ServerResponse.createByError("品类名称更新失败");
    }

    //获取品类子节点(平级)
    @Override
    public ServerResponse<List<Category>> getParallelCategory(Integer parentId) {

        List<Category> categories = categoryMapper.getParallelCategory(parentId);
        if (CollectionUtils.isEmpty(categories)) {
            return ServerResponse.createByError("未找到相应的子类");
        }
        return ServerResponse.createByError(categories);
    }

    //获取品类子节点(递归)
    @Override
    public ServerResponse<List<Integer>> getDeepCategory(Integer categoryId) {
        Set<Category> categorySet = Sets.newHashSet();
        getDeepMethod(categorySet, categoryId);
        List<Integer> idList = Lists.newArrayList();
        for (Category categoryItem : categorySet) {
            idList.add(categoryItem.getId());
        }
        return ServerResponse.createBySuccess(idList);
    }

    //获取递归算法
    public Set<Category> getDeepMethod(Set<Category> categorySet, Integer categoryId) {
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if (category != null) {
            categorySet.add(category);
        }
        List<Category> categoryList = categoryMapper.getParallelCategory(categoryId);
        for (Category categoryItem : categoryList) {
            getDeepMethod(categorySet, categoryItem.getId());
        }

        return categorySet;
    }

}
