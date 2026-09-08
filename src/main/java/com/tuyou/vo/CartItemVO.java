package com.tuyou.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 购物车条目（含产品/日期/价格快照 + 小计）
 */
@Data
public class CartItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private LocalDate departDate;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
