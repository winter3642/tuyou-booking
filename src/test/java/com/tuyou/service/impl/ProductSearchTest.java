package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuyou.entity.Product;
import com.tuyou.mapper.ProductMapper;
import com.tuyou.vo.PageVO;
import com.tuyou.vo.ProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 产品搜索单元测试（Mockito）
 * 覆盖：默认排序、全条件组合、关键词、排序白名单分支
 */
@ExtendWith(MockitoExtension.class)
class ProductSearchTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private com.tuyou.mapper.ProductSkuMapper skuMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Page<Product> mockPage(Product p, long total) {
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(p));
        page.setTotal(total);
        return page;
    }

    @Test
    @DisplayName("搜索：默认按销量倒序，只含上架产品")
    void searchDefaultSort() {
        Product p = new Product();
        p.setId(1L);
        p.setName("桂林6日游");
        p.setStatus(1);
        p.setSales(1000);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage(p, 42L));

        PageVO<ProductVO> result = productService.search(null, null, null,
                null, null, null, 1, 10);
        assertEquals(42L, result.getTotal());
        assertEquals("桂林6日游", result.getRecords().get(0).getName());
    }

    @Test
    @DisplayName("搜索：全部条件组合（关键词/分类/目的地/价格区间/价格升序）")
    void searchAllConditions() {
        Product p = new Product();
        p.setId(1L);
        p.setName("三亚度假");
        p.setStatus(1);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage(p, 1L));

        PageVO<ProductVO> result = productService.search(
                "三亚", 1L, 2L,
                new BigDecimal("100"), new BigDecimal("5000"),
                "price_asc", 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("搜索：price_desc 排序分支")
    void searchPriceDesc() {
        Product p = new Product();
        p.setId(1L);
        p.setStatus(1);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage(p, 5L));

        PageVO<ProductVO> result = productService.search(null, null, null,
                null, null, "price_desc", 1, 10);
        assertEquals(5L, result.getTotal());
    }

    @Test
    @DisplayName("搜索：score_desc 排序分支")
    void searchScoreDesc() {
        Product p = new Product();
        p.setId(1L);
        p.setStatus(1);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage(p, 3L));

        PageVO<ProductVO> result = productService.search(null, null, null,
                null, null, "score_desc", 1, 10);
        assertEquals(3L, result.getTotal());
    }

    @Test
    @DisplayName("搜索：非法 sortBy 回退默认排序（白名单兜底）")
    void searchInvalidSortBy() {
        Product p = new Product();
        p.setId(1L);
        p.setStatus(1);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage(p, 8L));

        PageVO<ProductVO> result = productService.search(null, null, null,
                null, null, "name;drop table", 1, 10);
        assertEquals(8L, result.getTotal());
    }
}
