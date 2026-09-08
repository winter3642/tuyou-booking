package com.tuyou.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品列表项
 */
@Data
public class ProductVO {
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
}
