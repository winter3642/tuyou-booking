package com.tuyou.controller;

import com.tuyou.common.Result;
import com.tuyou.dto.CartDTO;
import com.tuyou.interceptor.LoginUserHolder;
import com.tuyou.service.CartService;
import com.tuyou.vo.CartItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口（全部需登录：/api/cart/** 未被拦截器放行）
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** 我的购物车列表 */
    @GetMapping
    public Result<List<CartItemVO>> list() {
        return Result.ok(cartService.list(LoginUserHolder.get()));
    }

    /** 加购（同 sku 数量累加） */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CartDTO dto) {
        cartService.add(LoginUserHolder.get(), dto);
        return Result.ok();
    }

    /** 改数量（校验归属） */
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(LoginUserHolder.get(), id, quantity);
        return Result.ok();
    }

    /** 删除（校验归属） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(LoginUserHolder.get(), id);
        return Result.ok();
    }
}
