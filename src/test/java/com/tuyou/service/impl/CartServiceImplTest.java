package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.common.BizException;
import com.tuyou.dto.CartDTO;
import com.tuyou.entity.Cart;
import com.tuyou.entity.Product;
import com.tuyou.entity.ProductSku;
import com.tuyou.mapper.CartMapper;
import com.tuyou.mapper.ProductMapper;
import com.tuyou.mapper.ProductSkuMapper;
import com.tuyou.vo.CartItemVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 购物车单元测试
 * 覆盖：幂等加购（新增/累加/上限）、SKU 不存在、列表组装、越权防护
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductSkuMapper skuMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private ProductSku sku(Long id, Long productId) {
        ProductSku s = new ProductSku();
        s.setId(id);
        s.setProductId(productId);
        s.setDepartDate(LocalDate.of(2026, 10, 1));
        s.setPrice(new BigDecimal("2999.00"));
        return s;
    }

    @Test
    @DisplayName("加购：sku 不存在抛 404")
    void addSkuNotFound() {
        when(skuMapper.selectById(999L)).thenReturn(null);
        CartDTO dto = new CartDTO();
        dto.setSkuId(999L);
        dto.setQuantity(1);
        BizException ex = assertThrows(BizException.class, () -> cartService.add(1L, dto));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("加购：新 sku 插入一行")
    void addNew() {
        when(skuMapper.selectById(10L)).thenReturn(sku(10L, 100L));
        when(cartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        CartDTO dto = new CartDTO();
        dto.setSkuId(10L);
        dto.setQuantity(2);
        cartService.add(1L, dto);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(10L, captor.getValue().getSkuId());
        assertEquals(2, captor.getValue().getQuantity());
    }

    @Test
    @DisplayName("加购：已存在则数量累加而非重复插入")
    void addExistingAccumulate() {
        when(skuMapper.selectById(10L)).thenReturn(sku(10L, 100L));
        Cart existing = new Cart();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setSkuId(10L);
        existing.setQuantity(3);
        when(cartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        CartDTO dto = new CartDTO();
        dto.setSkuId(10L);
        dto.setQuantity(2);
        cartService.add(1L, dto);

        assertEquals(5, existing.getQuantity());
        verify(cartMapper).updateById(existing);
        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    @DisplayName("加购：累加后超过 99 抛参数错误")
    void addOverLimit() {
        when(skuMapper.selectById(10L)).thenReturn(sku(10L, 100L));
        Cart existing = new Cart();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setSkuId(10L);
        existing.setQuantity(98);
        when(cartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        CartDTO dto = new CartDTO();
        dto.setSkuId(10L);
        dto.setQuantity(5);
        assertThrows(BizException.class, () -> cartService.add(1L, dto));
    }

    @Test
    @DisplayName("列表：批量组装 SKU 与产品，小计=价格×数量")
    void listOk() {
        Cart c1 = new Cart();
        c1.setId(1L);
        c1.setUserId(1L);
        c1.setSkuId(10L);
        c1.setQuantity(2);
        when(cartMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(c1));

        ProductSku sku = sku(10L, 100L);
        when(skuMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(sku));
        Product p = new Product();
        p.setId(100L);
        p.setName("桂林6日游");
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(p));

        List<CartItemVO> result = cartService.list(1L);
        assertEquals(1, result.size());
        assertEquals("桂林6日游", result.get(0).getProductName());
        assertEquals(LocalDate.of(2026, 10, 1), result.get(0).getDepartDate());
        assertEquals(2, result.get(0).getQuantity());
        assertEquals(new BigDecimal("5998.00"), result.get(0).getSubtotal());
    }

    @Test
    @DisplayName("改数量：不是本人购物车抛 403（防越权）")
    void updateQuantityForbidden() {
        Cart other = new Cart();
        other.setId(1L);
        other.setUserId(999L);
        when(cartMapper.selectById(1L)).thenReturn(other);

        BizException ex = assertThrows(BizException.class,
                () -> cartService.updateQuantity(1L, 1L, 5));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("改数量：本人正常更新，非法数量拒绝")
    void updateQuantityOkAndInvalid() {
        Cart mine = new Cart();
        mine.setId(1L);
        mine.setUserId(1L);
        mine.setSkuId(10L);
        mine.setQuantity(1);
        when(cartMapper.selectById(1L)).thenReturn(mine);

        cartService.updateQuantity(1L, 1L, 7);
        assertEquals(7, mine.getQuantity());
        verify(cartMapper).updateById(mine);

        assertThrows(BizException.class, () -> cartService.updateQuantity(1L, 1L, 0));
        assertThrows(BizException.class, () -> cartService.updateQuantity(1L, 1L, 100));
    }

    @Test
    @DisplayName("删除：非本人 403，本人删除成功")
    void deleteOwnership() {
        Cart other = new Cart();
        other.setId(2L);
        other.setUserId(999L);
        when(cartMapper.selectById(2L)).thenReturn(other);
        assertThrows(BizException.class, () -> cartService.delete(1L, 2L));

        Cart mine = new Cart();
        mine.setId(1L);
        mine.setUserId(1L);
        when(cartMapper.selectById(1L)).thenReturn(mine);
        cartService.delete(1L, 1L);
        verify(cartMapper).deleteById(1L);
    }

    @Test
    @DisplayName("列表：空购物车直接返回空列表，不发批量查询")
    void listEmpty() {
        when(cartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<CartItemVO> result = cartService.list(1L);

        assertEquals(0, result.size());
        verify(skuMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    @DisplayName("列表：SKU 已删除则跳过该行（不抛异常），产品缺失显示已失效")
    void listSkuDeletedAndProductMissing() {
        Cart c1 = new Cart();
        c1.setId(1L);
        c1.setUserId(1L);
        c1.setSkuId(10L);
        c1.setQuantity(1);
        // SKU 已删：批量查询返回空
        when(cartMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(c1));
        when(skuMapper.selectBatchIds(anyCollection())).thenReturn(Collections.emptyList());

        List<CartItemVO> result = cartService.list(1L);

        assertEquals(0, result.size(), "失效 SKU 行应被跳过");

        // 第二段：SKU 在、产品被删 → 名称兜底"已失效产品"
        when(skuMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(sku(10L, 100L)));
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(Collections.emptyList());

        List<CartItemVO> result2 = cartService.list(1L);
        assertEquals(1, result2.size());
        assertEquals("已失效产品", result2.get(0).getProductName());
    }
}
