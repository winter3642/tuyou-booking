package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuyou.common.BizException;
import com.tuyou.dto.ProductDTO;
import com.tuyou.entity.Product;
import com.tuyou.entity.ProductSku;
import com.tuyou.mapper.ProductMapper;
import com.tuyou.mapper.ProductSkuMapper;
import com.tuyou.vo.PageVO;
import com.tuyou.vo.ProductDetailVO;
import com.tuyou.vo.ProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品服务单元测试（Mockito mock Mapper）
 * 验证：分页、详情含 SKU、404、新增回填 id、上下架
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper skuMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("分页：返回 total 与 VO 列表")
    void pageOk() {
        Product p = new Product();
        p.setId(1L);
        p.setName("测试产品");
        p.setPrice(new BigDecimal("100.00"));
        p.setStatus(1);

        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(p));
        page.setTotal(999L);
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageVO<ProductVO> result = productService.page(1, 10, 1);
        assertEquals(999L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("测试产品", result.getRecords().get(0).getName());
    }

    @Test
    @DisplayName("详情：产品不存在抛 404")
    void detailNotFound() {
        when(productMapper.selectById(99L)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> productService.detail(99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("详情：返回产品信息与 SKU 日期价格列表")
    void detailOk() {
        Product p = new Product();
        p.setId(1L);
        p.setName("桂林6日游");
        p.setStatus(1);
        when(productMapper.selectById(1L)).thenReturn(p);

        ProductSku s1 = new ProductSku();
        s1.setId(1L);
        s1.setProductId(1L);
        s1.setDepartDate(LocalDate.of(2026, 10, 1));
        s1.setStock(50);
        s1.setPrice(new BigDecimal("2999.00"));
        when(skuMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(s1));

        ProductDetailVO vo = productService.detail(1L);
        assertEquals("桂林6日游", vo.getName());
        assertEquals(1, vo.getSkuList().size());
        assertEquals(LocalDate.of(2026, 10, 1), vo.getSkuList().get(0).getDepartDate());
        assertEquals(50, vo.getSkuList().get(0).getStock());
    }

    @Test
    @DisplayName("新增：返回 MP 回填的自增 id")
    void createOk() {
        ProductDTO dto = new ProductDTO();
        dto.setCategoryId(1L);
        dto.setDestinationId(1L);
        dto.setName("测试新品");
        dto.setPrice(new BigDecimal("888.00"));
        dto.setStatus(1);

        // 模拟 MyBatis-Plus insert 后的主键回填行为
        doAnswer(inv -> {
            Product product = inv.getArgument(0);
            product.setId(100001L);
            return 1;
        }).when(productMapper).insert(any(Product.class));

        Long id = productService.create(dto);
        assertEquals(100001L, id);
    }

    @Test
    @DisplayName("上下架：产品不存在抛 404；存在则更新状态")
    void updateStatus() {
        when(productMapper.selectById(1L)).thenReturn(null);
        assertThrows(BizException.class, () -> productService.updateStatus(1L, 0));

        Product p = new Product();
        p.setId(2L);
        p.setStatus(1);
        when(productMapper.selectById(2L)).thenReturn(p);

        productService.updateStatus(2L, 0);
        assertEquals(0, p.getStatus());
        verify(productMapper).updateById(p);
    }
}
