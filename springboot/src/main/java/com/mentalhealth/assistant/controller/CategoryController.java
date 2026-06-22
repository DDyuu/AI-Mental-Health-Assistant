package com.mentalhealth.assistant.controller;

import com.mentalhealth.assistant.common.Result;
import com.mentalhealth.assistant.entity.Category;
import com.mentalhealth.assistant.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/category/tree")
    public Result<List<Category>> getCategoryTree() {
        List<Category> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }
}
