package com.tuyou.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品主表
 */
@Data
@TableName("t_product")
public class Product {

    @TableId(type = IdType.AUTO)
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
