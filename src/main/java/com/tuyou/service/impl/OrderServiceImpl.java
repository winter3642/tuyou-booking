package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.common.BizException;
import com.tuyou.common.ResultCode;
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
import com.tuyou.service.OrderService;
import com.tuyou.utils.SnowflakeIdGenerator;
import com.tuyou.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartMapper cartMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO createOrder(Long userId, CreateOrderDTO dto) {
        // 1. 校验购物车：存在 + 归属本人（防水平越权）
        if (dto.getCartIds() == null || dto.getCartIds().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请选择要结算的商品");
        }
        List<Cart> carts = cartMapper.selectBatchIds(dto.getCartIds());
        if (carts.size() != dto.getCartIds().size()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "部分商品不存在，请刷新后重试");
        }
        for (Cart c : carts) {
            if (!c.getUserId().equals(userId)) {
                throw new BizException(ResultCode.FORBIDDEN);
            }
        }

        // 2. 条件扣库存（防超卖）：UPDATE ... SET stock=stock-? WHERE id=? AND stock>=?
        //    影响行数=0 说明库存不足 → 抛异常回滚整个事务
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (Cart c : carts) {
            ProductSku sku = skuMapper.selectById(c.getSkuId());
            if (sku == null) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "产品日期已失效，请重新选择");
            }
            int rows = skuMapper.deductStock(c.getSkuId(), c.getQuantity());
            if (rows == 0) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "库存不足，请调整数量");
            }
            Product product = productMapper.selectById(sku.getProductId());
            OrderItem item = new OrderItem();
            item.setProductId(sku.getProductId());
            item.setSkuId(c.getSkuId());
            item.setProductName(product != null ? product.getName() : "未知产品");
            item.setPrice(sku.getPrice());
            item.setQuantity(c.getQuantity());
            items.add(item);
            total = total.add(sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }

        // 3. 写订单（雪花ID，非自增）
        long orderId = snowflakeIdGenerator.nextId();
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNo("TO" + orderId);
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus(0);
        orderMapper.insert(order);

        // 4. 写明细（产品名/价格快照）
        for (OrderItem item : items) {
            item.setOrderId(orderId);
            orderItemMapper.insert(item);
        }

        // 5. 清购物车（已结算条目）
        cartMapper.deleteBatchIds(dto.getCartIds());

        // 6. 生成支付单（mock 渠道，待支付）
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPayNo("P" + orderId);
        payment.setChannel("mock");
        payment.setStatus(0);
        payment.setAmount(total);
        paymentMapper.insert(payment);

        return buildDetail(order, items);
    }

    @Override
    public List<Order> list(Long userId) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime));
    }

    @Override
    public OrderDetailVO detail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        return buildDetail(order, items);
    }

    private OrderDetailVO buildDetail(Order order, List<OrderItem> items) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items);
        return vo;
    }
}
