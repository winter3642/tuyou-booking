package com.tuyou.service.impl;

import com.tuyou.service.StockRedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 库存服务测试：Lua 脚本返回值映射 + 初始化幂等 + 回滚
 */
@ExtendWith(MockitoExtension.class)
class StockRedisServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private StockRedisServiceImpl stockService;

    @Test
    @DisplayName("初始化：SETNX 幂等写入")
    void initStock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        stockService.initStock(1L, 100);
        verify(valueOps).setIfAbsent("stock:sku:1", "100");
    }

    @Test
    @DisplayName("Lua 返回 1：扣减成功")
    void tryDeductSuccess() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        assertEquals(1, stockService.tryDeduct(1L, 2));
    }

    @Test
    @DisplayName("Lua 返回 0：库存不足")
    void tryDeductNotEnough() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);
        assertEquals(0, stockService.tryDeduct(1L, 2));
    }

    @Test
    @DisplayName("Lua 返回 -1：未初始化")
    void tryDeductNotInit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(-1L);
        assertEquals(-1, stockService.tryDeduct(1L, 2));
    }

    @Test
    @DisplayName("回滚：INCRBY 归还库存")
    void rollback() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        stockService.rollback(1L, 2);
        verify(valueOps).increment("stock:sku:1", 2);
    }

    @Test
    @DisplayName("查询：未初始化返回 null")
    void getStockNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("stock:sku:9")).thenReturn(null);
        assertNull(stockService.getStock(9L));
    }
}
