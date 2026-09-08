package com.tuyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuyou.common.BizException;
import com.tuyou.common.ResultCode;
import com.tuyou.dto.CartDTO;
import com.tuyou.entity.Cart;
import com.tuyou.entity.Product;
import com.tuyou.entity.ProductSku;
import com.tuyou.mapper.CartMapper;
import com.tuyou.mapper.ProductMapper;
import com.tuyou.mapper.ProductSkuMapper;
import com.tuyou.service.CartService;
import com.tuyou.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int MAX_QUANTITY = 99;

    private final CartMapper cartMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public void add(Long userId, CartDTO dto) {
        // 校验 SKU（产品+日期）真实存在
        ProductSku sku = skuMapper.selectById(dto.getSkuId());
        if (sku == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "产品日期不存在");
        }
        // 幂等加购：同 user+sku 唯一，先查后更
        Cart existing = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, dto.getSkuId()));
        if (existing != null) {
            int newQty = existing.getQuantity() + dto.getQuantity();
            if (newQty > MAX_QUANTITY) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "购物车数量超出上限");
            }
            existing.setQuantity(newQty);
            cartMapper.updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setSkuId(dto.getSkuId());
            cart.setQuantity(dto.getQuantity());
            cartMapper.insert(cart);
        }
    }

    @Override
    public List<CartItemVO> list(Long userId) {
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
        if (carts.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量取 SKU 与产品，避免 N+1（面试点：IN 查询替代逐条查询）
        List<Long> skuIds = carts.stream().map(Cart::getSkuId).collect(Collectors.toList());
        Map<Long, ProductSku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        List<Long> productIds = skuMap.values().stream()
                .map(ProductSku::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<CartItemVO> result = new ArrayList<>();
        for (Cart c : carts) {
            ProductSku sku = skuMap.get(c.getSkuId());
            if (sku == null) {
                continue; // SKU 已删除则跳过该行
            }
            CartItemVO vo = new CartItemVO();
            vo.setId(c.getId());
            vo.setSkuId(c.getSkuId());
            vo.setQuantity(c.getQuantity());
            vo.setDepartDate(sku.getDepartDate());
            vo.setPrice(sku.getPrice());
            vo.setProductId(sku.getProductId());
            Product p = productMap.get(sku.getProductId());
            vo.setProductName(p != null ? p.getName() : "已失效产品");
            vo.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
            result.add(vo);
        }
        return result;
    }

    @Override
    public void updateQuantity(Long userId, Long cartId, Integer quantity) {
        Cart cart = requireOwned(userId, cartId);
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "数量必须在 1-99 之间");
        }
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
    }

    @Override
    public void delete(Long userId, Long cartId) {
        Cart cart = requireOwned(userId, cartId);
        cartMapper.deleteById(cart.getId());
    }

    /** 归属校验：不存在 404，非本人 403（防水平越权） */
    private Cart requireOwned(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!cart.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return cart;
    }
}
