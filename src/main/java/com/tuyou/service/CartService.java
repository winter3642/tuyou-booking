package com.tuyou.service;

import com.tuyou.dto.CartDTO;
import com.tuyou.vo.CartItemVO;

import java.util.List;

public interface CartService {

    /** 加购：同 user+sku 已存在则数量累加，否则新增 */
    void add(Long userId, CartDTO dto);

    /** 我的购物车列表（批量取 SKU/产品组装，避免 N+1） */
    List<CartItemVO> list(Long userId);

    /** 改数量（校验归属） */
    void updateQuantity(Long userId, Long cartId, Integer quantity);

    /** 删除（校验归属） */
    void delete(Long userId, Long cartId);
}
