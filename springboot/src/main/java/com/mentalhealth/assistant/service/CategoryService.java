package com.mentalhealth.assistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mentalhealth.assistant.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 获取分类树（含层级结构）
     */
    List<Category> getCategoryTree();
}
