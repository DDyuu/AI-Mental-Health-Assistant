package com.mentalhealth.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mentalhealth.assistant.entity.Category;
import com.mentalhealth.assistant.mapper.CategoryMapper;
import com.mentalhealth.assistant.service.CategoryService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    @Cacheable(value = "category:tree", unless = "#result == null || #result.isEmpty()")
    public List<Category> getCategoryTree() {
        // 查询所有启用的分类
        List<Category> allCategories = baseMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );

        // 获取所有根节点（parent_id 为 null 的分类）
        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        // 为每个根节点递归构建子树
        for (Category root : rootCategories) {
            root.setChildren(buildChildren(root.getId(), allCategories));
        }

        return rootCategories;
    }

    /**
     * 递归构建子分类
     */
    private List<Category> buildChildren(Integer parentId, List<Category> allCategories) {
        List<Category> children = new ArrayList<>();
        for (Category category : allCategories) {
            if (parentId.equals(category.getParentId())) {
                category.setChildren(buildChildren(category.getId(), allCategories));
                children.add(category);
            }
        }
        return children.isEmpty() ? null : children;
    }
}
