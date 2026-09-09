package com.tuyou.service.impl;

import com.tuyou.service.StockRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 库存实现：Lua 脚本保证"判断+扣减"原子性
 * 单线程 Redis 执行脚本期间不会被其他命令插入，天然防超卖
 */
@Service
@RequiredArgsConstructor
public class StockRedisServiceImpl implements StockRedisService {

    private static final String STOCK_KEY_PREFIX = "stock:sku:";

    /**
     * KEYS[1] = 库存 key，ARGV[1] = 购买数量
     * 返回：1 成功；0 库存不足；-1 未初始化
     */
    private static final String DEDUCT_LUA =
            "local stock = tonumber(redis.call('get', KEYS[1])) " +
            "if stock == nil then return -1 end " +
            "if stock >= tonumber(ARGV[1]) then " +
            "  redis.call('decrby', KEYS[1], ARGV[1]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> deductScript =
            new DefaultRedisScript<>(DEDUCT_LUA, Long.class);

    @Override
    public void initStock(Long skuId, Integer stock) {
        // SETNX：首次才写入，不覆盖已扣减中的库存
        redisTemplate.opsForValue().setIfAbsent(STOCK_KEY_PREFIX + skuId, String.valueOf(stock));
    }

    @Override
    public int tryDeduct(Long skuId, Integer quantity) {
        Long result = redisTemplate.execute(deductScript,
                List.of(STOCK_KEY_PREFIX + skuId), String.valueOf(quantity));
        return result == null ? -1 : result.intValue();
    }

    @Override
    public void rollback(Long skuId, Integer quantity) {
        redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + skuId, quantity);
    }

    @Override
    public Integer getStock(Long skuId) {
        String v = redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + skuId);
        return v == null ? null : Integer.parseInt(v);
    }
}
