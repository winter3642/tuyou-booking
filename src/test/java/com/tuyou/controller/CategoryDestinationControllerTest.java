package com.tuyou.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 分类/目的地接口 MockMvc 测试（W3D4）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
// Windows 下 Spring 默认用平台编码(GBK)读脚本，必须显式 UTF-8，否则中文 INSERT 进库是乱码（踩坑记录）
@SqlConfig(encoding = "UTF-8")
class CategoryDestinationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("分类树：返回嵌套结构")
    void categoryTree() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("跟团游"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("国内"));
    }

    @Test
    @DisplayName("目的地列表：按热门排序返回")
    void destinationList() throws Exception {
        mockMvc.perform(get("/api/destinations"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("桂林"));
    }
}
