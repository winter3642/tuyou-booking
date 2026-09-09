package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.common.BizException;
import com.tuyou.dto.CreateOrderDTO;
import com.tuyou.entity.Cart;
import com.tuyou.entity.Order;
import com.tuyou.entity.OrderItem;
import com.tuyou.entity.Payment;
import com.tuyou.entity.Product;
import com.tuyou.entity.ProductSku;
import com.tuyou.mapper.CartMapper;
import com.tuyou.mapper.OrderItemMapper;
import com.tuyou.mapper.OrderMapper;
import com.tuyou.mapper.PaymentMapper;
import com.tuyou.mapper.ProductMapper;
import com.tuyou.mapper.ProductSkuMapper;
import com.tuyou.service.StockRedisService;
import com.tuyou.utils.SnowflakeIdGenerator;
import com.tuyou.vo.OrderDetailVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单链路单元测试（W2D4：含 Redis 预扣 + 失败回滚）
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private CartMapper cartMapper;
    @Mock
    private ProductSkuMapper skuMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Mock
    private StockRedisService stockRedisService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Cart cart(Long id, Long userId, Long skuId, int qty) {
        Cart c = new Cart();
        c.setId(id);
        c.setUserId(userId);
        c.setSkuId(skuId);
        c.setQuantity(qty);
        return c;
    }

    private ProductSku sku(Long id) {
        ProductSku s = new ProductSku();
        s.setId(id);
        s.setProductId(100L);
        s.setDepartDate(LocalDate.of(2026, 10, 1));
        s.setStock(1000);
        s.setPrice(new BigDecimal("1000.00"));
        return s;
    }

    private CreateOrderDTO dto(Long... cartIds) {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setCartIds(new ArrayList<>(List.of(cartIds)));
        return dto;
    }

    @Test
    @DisplayName("下单：空购物车选择抛参数错误")
    void emptyCartIds() {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setCartIds(Collections.emptyList());
        assertThrows(BizException.class, () -> orderService.createOrder(1L, dto));
    }

    @Test
    @DisplayName("下单：包含他人购物车条目抛 403（防越权）")
    void forbiddenCart() {
        when(cartMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(cart(9L, 999L, 1L, 1)));
        BizException ex = assertThrows(BizException.class, () -> orderService.createOrder(1L, dto(9L)));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("下单：Redis 预扣库存不足直接拒绝（不落订单、不产生回滚）")
    void redisStockNotEnough() {
        when(cartMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(cart(1L, 1L, 10L, 50)));
        when(stockRedisService.tryDeduct(10L, 50)).thenReturn(0);

        BizException ex = assertThrows(BizException.class, () -> orderService.createOrder(1L, dto(1L)));
        assertEquals(400, ex.getCode());
        verify(orderMapper, never()).insert(any(Order.class));
        verify(stockRedisService, never()).rollback(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("下单：Redis 未初始化时从 DB 加载并重试")
    void redisNotInitThenOk() {
        when(cartMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(cart(1L, 1L, 10L, 2)));
        when(stockRedisService.tryDeduct(10L, 2)).thenReturn(-1).thenReturn(1);
        when(skuMapper.selectById(10L)).thenReturn(sku(10L));
        when(skuMapper.deductStock(10L, 2)).thenReturn(1);
        Product p = new Product();
        p.setId(100L);
        p.setName("桂林6日游");
        when(productMapper.selectById(100L)).thenReturn(p);
        when(snowflakeIdGenerator.nextId()).thenReturn(123L);

        orderService.createOrder(1L, dto(1L));

        verify(stockRedisService).initStock(10L, 1000);
        // 真实流程：71 行第一次预扣返回 -1（未初始化）→ 初始化后 79 行重试第二次
        verify(stockRedisService, times(2)).tryDeduct(10L, 2);
    }

    @Test
    @DisplayName("下单：Redis 预扣成功但 DB 扣减失败 → 回滚 Redis 且不写订单")
    void dbFailRollbackRedis() {
        when(cartMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(cart(1L, 1L, 10L, 2)));
        when(stockRedisService.tryDeduct(10L, 2)).thenReturn(1);
        when(skuMapper.selectById(10L)).thenReturn(sku(10L));
        when(skuMapper.deductStock(10L, 2)).thenReturn(0); // DB 扣减失败

        BizException ex = assertThrows(BizException.class, () -> orderService.createOrder(1L, dto(1L)));
        assertEquals(400, ex.getCode());
        // Redis 库存已归还，订单未落库
        verify(stockRedisService).rollback(10L, 2);
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("下单：正常链路——Redis预扣→DB兜底→雪花订单→明细→清购物车→支付单")
    void createOk() {
        Cart c1 = cart(1L, 1L, 10L, 2);
        Cart c2 = cart(2L, 1L, 11L, 1);
        when(cartMapper.selectBatchIds(anyCollection())).thenReturn(List.of(c1, c2));
        when(stockRedisService.tryDeduct(anyLong(), any(Integer.class))).thenReturn(1);

        ProductSku sku1 = sku(10L);
        sku1.setPrice(new BigDecimal("1000.00"));
        ProductSku sku2 = sku(11L);
        sku2.setPrice(new BigDecimal("500.00"));
        when(skuMapper.selectById(10L)).thenReturn(sku1);
        when(skuMapper.selectById(11L)).thenReturn(sku2);
        when(skuMapper.deductStock(anyLong(), any(Integer.class))).thenReturn(1);

        Product p = new Product();
        p.setId(100L);
        p.setName("桂林6日游");
        when(productMapper.selectById(anyLong())).thenReturn(p);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);

        OrderDetailVO vo = orderService.createOrder(1L, dto(1L, 2L));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals(123456789L, orderCaptor.getValue().getId());
        assertEquals("TO123456789", orderCaptor.getValue().getOrderNo());
        assertEquals(new BigDecimal("2500.00"), orderCaptor.getValue().getTotalAmount());
        assertEquals(0, orderCaptor.getValue().getStatus());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        assertEquals(123456789L, itemCaptor.getAllValues().get(0).getOrderId());
        assertEquals("桂林6日游", itemCaptor.getAllValues().get(0).getProductName());

        ArgumentCaptor<Payment> payCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(payCaptor.capture());
        assertEquals(123456789L, payCaptor.getValue().getOrderId());
        assertEquals(new BigDecimal("2500.00"), payCaptor.getValue().getAmount());

        verify(cartMapper).deleteBatchIds(List.of(1L, 2L));
        assertEquals(new BigDecimal("2500.00"), vo.getTotalAmount());
        assertEquals(2, vo.getItems().size());
    }

    @Test
    @DisplayName("详情：看他人订单抛 403")
    void detailForbidden() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(999L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        assertThrows(BizException.class, () -> orderService.detail(1L, 1L));
    }

    @Test
    @DisplayName("详情：本人订单返回明细")
    void detailOk() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setOrderNo("TO1");
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        OrderDetailVO vo = orderService.detail(1L, 1L);
        assertEquals("TO1", vo.getOrderNo());
    }
}
