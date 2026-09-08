package com.tuyou.service;

import com.tuyou.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    /** 分类树（三级） */
    List<CategoryVO> tree();
}
