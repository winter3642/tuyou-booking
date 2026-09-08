package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.entity.Category;
import com.tuyou.mapper.CategoryMapper;
import com.tuyou.service.CategoryService;
import com.tuyou.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> tree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        // 按 parentId 分组，一次遍历构建树
        Map<Long, List<Category>> byParent = all.stream()
                .collect(Collectors.groupingBy(Category::getParentId));

        List<CategoryVO> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() != null && c.getParentId() == 0L) {
                roots.add(toVO(c, byParent));
            }
        }
        return roots;
    }

    private CategoryVO toVO(Category c, Map<Long, List<Category>> byParent) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(c, vo);
        List<Category> children = byParent.getOrDefault(c.getId(), Collections.emptyList());
        vo.setChildren(children.stream()
                .map(ch -> toVO(ch, byParent))
                .collect(Collectors.toList()));
        return vo;
    }
}
