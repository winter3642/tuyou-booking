package com.tuyou.service;

import com.tuyou.dto.CreateOrderDTO;
import com.tuyou.entity.Order;
import com.tuyou.vo.OrderDetailVO;

import java.util.List;

public interface OrderService {

    /** 下单：事务内 校验购物车 → 条件扣库存 → 写订单/明细 → 清购物车 → 生成支付单 */
    OrderDetailVO createOrder(Long userId, CreateOrderDTO dto);

    /** 我的订单列表 */
    List<Order> list(Long userId);

    /** 订单详情（校验归属） */
    OrderDetailVO detail(Long userId, Long orderId);
}
