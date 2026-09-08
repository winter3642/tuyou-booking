package com.tuyou.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车表（按 SKU 粒度：产品+出行日期）
 * 唯一索引 (user_id, sku_id) 保证同用户同日期只一行，数量累加
 */
@Data
@TableName("t_cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private LocalDateTime createTime;
}
