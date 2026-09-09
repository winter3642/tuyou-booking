package com.tuyou.controller;

import com.tuyou.utils.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 购物车接口 MockMvc 测试（W3D4）
 * 全链路：未登录 401 → 加购 → 列表 → 改数量 → 删除；越权 403
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
// Windows 下 Spring 默认用平台编码(GBK)读脚本，必须显式 UTF-8，否则中文 INSERT 进库是乱码（踩坑记录）
@SqlConfig(encoding = "UTF-8")
class CartControllerTest {

    private static final String USER_TOKEN = JwtUtil.generateToken(1L, "test01");
    private static final String ADMIN_TOKEN = JwtUtil.generateToken(2L, "admin");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未登录访问购物车：401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("加购 → 列表 → 改数量 → 删除 全链路")
    void fullLifecycle() throws Exception {
        // 加购 sku 800001 × 2
        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":800001,\"quantity\":2}"))
                .andExpect(jsonPath("$.code").value(0));

        // 列表：1 条，小计 = 2999 × 2
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("桂林6日游测试产品"))
                .andExpect(jsonPath("$.data[0].quantity").value(2))
                .andExpect(jsonPath("$.data[0].subtotal").value(5998.00));

        // 改数量 5
        mockMvc.perform(put("/api/cart/1")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("quantity", "5"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(jsonPath("$.data[0].quantity").value(5));

        // 删除
        mockMvc.perform(delete("/api/cart/1")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("加购不存在 SKU：404")
    void addSkuNotFound() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":999999,\"quantity\":1}"))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("改他人购物车条目：403 防越权")
    void updateOthersCart() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":800001,\"quantity\":1}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/api/cart/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("quantity", "9"))
                .andExpect(jsonPath("$.code").value(403));
    }
}
