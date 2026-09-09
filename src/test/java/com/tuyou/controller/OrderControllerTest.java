package com.tuyou.controller;

import com.tuyou.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 订单接口 MockMvc 测试（W3D4）
 * 全链路：未登录 401 → 加购 → 下单 → 列表 → 详情；越权 403；空购物车 400
 * 注意：下单会写 Redis 库存，BeforeEach 清理 stock:sku:* 避免跨测试残留
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
// Windows 下 Spring 默认用平台编码(GBK)读脚本，必须显式 UTF-8，否则中文 INSERT 进库是乱码（踩坑记录）
@SqlConfig(encoding = "UTF-8")
class OrderControllerTest {

    private static final String USER_TOKEN = JwtUtil.generateToken(1L, "test01");
    private static final String ADMIN_TOKEN = JwtUtil.generateToken(2L, "admin");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearRedisStock() {
        // 清掉测试 SKU 在 Redis 的库存缓存（DB 由 @Sql 重置）
        redisTemplate.delete("stock:sku:800001");
        redisTemplate.delete("stock:sku:800002");
    }

    @Test
    @DisplayName("未登录下单：401")
    void unauthorized() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartIds\":[1]}"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("下单空购物车选择：400")
    void emptyCartIds() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartIds\":[]}"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("下单全链路：加购 → 下单 → 列表 → 详情")
    void createAndQuery() throws Exception {
        // 加购 sku 800001 × 2
        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":800001,\"quantity\":2}"))
                .andExpect(jsonPath("$.code").value(0));

        // 下单（购物车条目 id=1）
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartIds\":[1]}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(5998.00))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.items[0].productName").value("桂林6日游测试产品"));

        // 订单列表
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1));

        // 订单详情（id=雪花ID，从 orderNo 抠：orderNo = "TO" + id，避免误匹配明细 id）
        String body = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andReturn().getResponse().getContentAsString();
        String orderId = body.replaceAll(".*\"orderNo\":\"TO(\\d+)\".*", "$1");
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    @DisplayName("查看他人订单详情：403 防越权")
    void detailForbidden() throws Exception {
        // test01 加购并下单
        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":800001,\"quantity\":1}"))
                .andExpect(jsonPath("$.code").value(0));
        String body = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartIds\":[1]}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String orderId = body.replaceAll(".*\"orderNo\":\"TO(\\d+)\".*", "$1");

        // admin 看 test01 的订单 → 403
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("订单详情不存在：404")
    void detailNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/999999")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(jsonPath("$.code").value(404));
    }
}
