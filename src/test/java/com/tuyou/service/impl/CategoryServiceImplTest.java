package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.entity.Category;
import com.tuyou.mapper.CategoryMapper;
import com.tuyou.vo.CategoryVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 分类服务单元测试（W3D4 补空白：Category 此前 0 覆盖）
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category cat(Long id, String name, Long parentId, int level) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setParentId(parentId);
        c.setLevel(level);
        c.setSort(1);
        return c;
    }

    @Test
    @DisplayName("分类树：一级 → 二级嵌套")
    void treeNested() {
        when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(cat(1L, "跟团游", 0L, 1), cat(2L, "国内", 1L, 2)));

        List<CategoryVO> tree = categoryService.tree();
        assertEquals(1, tree.size());
        assertEquals("跟团游", tree.get(0).getName());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("国内", tree.get(0).getChildren().get(0).getName());
    }

    @Test
    @DisplayName("分类树：空表返回空列表")
    void treeEmpty() {
        when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        assertTrue(categoryService.tree().isEmpty());
    }
}
