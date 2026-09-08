package com.tuyou.controller;

import com.tuyou.common.Result;
import com.tuyou.service.CategoryService;
import com.tuyou.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> tree() {
        return Result.ok(categoryService.tree());
    }
}
