package com.tuyou.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品详情（含 SKU 日期价格列表）
 */
@Data
public class ProductDetailVO {
    private Long id;
    private Long categoryId;
    private Long destinationId;
    private String name;
    private String subtitle;
    private BigDecimal price;
    private Integer sales;
    private BigDecimal score;
    private Integer status;
    private LocalDateTime createTime;
    private List<SkuVO> skuList;
}
