package com.tuyou.service;

/**
 * Redis 库存服务：高并发下的原子扣减（防超卖核心）
 * 通过 Lua 脚本把"读库存→判断→扣减"打成原子操作，避免并发读脏
 */
public interface StockRedisService {

    /**
     * 初始化某个 SKU 的 Redis 库存（SETNX 幂等：已存在不覆盖）
     */
    void initStock(Long skuId, Integer stock);

    /**
     * Lua 原子扣减
     *
     * @return 1=扣减成功；0=库存不足；-1=未初始化（需先 initStock）
     */
    int tryDeduct(Long skuId, Integer quantity);

    /**
     * 回滚：下单后续步骤失败时归还 Redis 库存
     */
    void rollback(Long skuId, Integer quantity);

    /**
     * 查询 Redis 剩余库存（未初始化返回 null）
     */
    Integer getStock(Long skuId);
}
