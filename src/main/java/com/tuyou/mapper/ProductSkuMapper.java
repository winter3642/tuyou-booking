package com.tuyou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuyou.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 条件扣库存（防超卖核心）：
     * 只有 stock >= quantity 才扣减，返回影响行数；0 表示库存不足或不存在。
     * 数据库行锁 + 条件更新保证并发下不会扣成负数。
     */
    @Update("UPDATE t_product_sku SET stock = stock - #{quantity} WHERE id = #{skuId} AND stock >= #{quantity}")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
