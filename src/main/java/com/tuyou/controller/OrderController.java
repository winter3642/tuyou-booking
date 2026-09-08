package com.tuyou.controller;

import com.tuyou.common.Result;
import com.tuyou.dto.CreateOrderDTO;
import com.tuyou.entity.Order;
import com.tuyou.interceptor.LoginUserHolder;
import com.tuyou.service.OrderService;
import com.tuyou.vo.OrderDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口（全部需登录）
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 下单（购物车结算） */
    @PostMapping
    public Result<OrderDetailVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.ok(orderService.createOrder(LoginUserHolder.get(), dto));
    }

    /** 我的订单列表 */
    @GetMapping
    public Result<List<Order>> list() {
        return Result.ok(orderService.list(LoginUserHolder.get()));
    }

    /** 订单详情（含明细，校验归属） */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(LoginUserHolder.get(), id));
    }
}
