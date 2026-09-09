package com.tuyou.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花 ID 生成器测试（W3D4 补全）
 * 覆盖：连续生成的唯一性与单调性、workerId/datacenterId 参数校验、时钟回拨拒绝
 */
class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("连续生成 1000 个 ID：全部唯一且单调递增（时间有序）")
    void uniqueAndMonotonic() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1L, 0L);
        Set<Long> ids = new HashSet<>();
        long prev = -1L;
        for (int i = 0; i < 1000; i++) {
            long id = gen.nextId();
            assertTrue(id > prev, "ID 必须单调递增（同一毫秒内靠序列号保证）");
            prev = id;
            ids.add(id);
        }
        assertEquals(1000, ids.size(), "1000 个 ID 必须全部唯一");
    }

    @Test
    @DisplayName("workerId 越界抛 IllegalArgumentException（0~31）")
    void invalidWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(32L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1L, 0L));
    }

    @Test
    @DisplayName("datacenterId 越界抛 IllegalArgumentException（0~31）")
    void invalidDatacenterId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(0L, 32L));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(0L, -1L));
    }

    @Test
    @DisplayName("时钟回拨：拒绝生成 ID，防止重复（面试点）")
    void clockRollback() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1L, 0L);
        gen.nextId(); // 先正常生成一次，让 lastTimestamp 被赋值
        // 反射把 lastTimestamp 拨到未来，模拟时钟回拨
        ReflectionTestUtils.setField(gen, "lastTimestamp", System.currentTimeMillis() + 5_000L);

        assertThrows(RuntimeException.class, gen::nextId);
    }
}
