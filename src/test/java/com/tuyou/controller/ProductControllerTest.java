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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 产品接口 MockMvc 测试（W3D4）
 * 公开查询（列表/搜索/详情）+ 管理端（admin 创建/上下架，普通用户 403）
 * token 直接用 JwtUtil 生成：admin 判定只看 username，无需 DB 造管理员
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
// Windows 下 Spring 默认用平台编码(GBK)读脚本，必须显式 UTF-8，否则中文 INSERT 进库是乱码（踩坑记录）
@SqlConfig(encoding = "UTF-8")
class ProductControllerTest {

    private static final String ADMIN_TOKEN = JwtUtil.generateToken(2L, "admin");
    private static final String USER_TOKEN = JwtUtil.generateToken(1L, "test01");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("产品列表：仅返回上架产品")
    void pageOk() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "1").param("size", "10").param("status", "1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].name").value("桂林6日游测试产品"));
    }

    @Test
    @DisplayName("产品搜索：FULLTEXT 关键词命中上架产品")
    void searchOk() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "桂林"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("桂林6日游测试产品"));
    }

    @Test
    @DisplayName("产品详情：返回基本信息与 SKU 列表")
    void detailOk() throws Exception {
        mockMvc.perform(get("/api/products/900001"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("桂林6日游测试产品"))
                .andExpect(jsonPath("$.data.skuList[0].stock").value(100));
    }

    @Test
    @DisplayName("产品详情不存在：404（并触发空值缓存）")
    void detailNotFound() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("管理端创建产品：未登录 401")
    void createUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":9001,\"destinationId\":9001,\"name\":\"新品\",\"price\":888.00}"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("管理端创建产品：普通用户 403")
    void createForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":9001,\"destinationId\":9001,\"name\":\"新品\",\"price\":888.00}"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("管理端创建产品：admin 成功返回新 id")
    void createOk() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":9001,\"destinationId\":9001,\"name\":\"测试新品\",\"subtitle\":\"x\",\"price\":888.00,\"status\":1}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("管理端上下架：admin 下架后列表不再返回")
    void updateStatusOk() throws Exception {
        mockMvc.perform(put("/api/admin/products/900001/status")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("status", "0"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/products").param("status", "1"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("管理端修改产品：普通用户 403")
    void updateForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/products/900001")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":9001,\"destinationId\":9001,\"name\":\"改名\",\"price\":888.00}"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("管理端修改产品：admin 成功后详情可见新值")
    void updateOk() throws Exception {
        mockMvc.perform(put("/api/admin/products/900001")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":9001,\"destinationId\":9001,\"name\":\"改名后产品\",\"price\":666.00}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/products/900001"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("改名后产品"))
                .andExpect(jsonPath("$.data.price").value(666.00));
    }
}
