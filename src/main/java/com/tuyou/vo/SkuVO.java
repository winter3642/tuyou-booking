package com.tuyou.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SKU 出参（某个出行日期的库存与价格）
 */
@Data
public class SkuVO {
    private Long id;
    private LocalDate departDate;
    private Integer stock;
    private BigDecimal price;
}
