package com.tuyou.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户接口 MockMvc 测试（W3D4）
 * 数据隔离：独立测试库 tuyou_test + @Sql 每方法前初始化/后清理
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
// Windows 下 Spring 默认用平台编码(GBK)读脚本，必须显式 UTF-8，否则中文 INSERT 进库是乱码（踩坑记录）
@SqlConfig(encoding = "UTF-8")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("注册成功：code=0")
    void registerOk() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\",\"phone\":\"13800000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("注册参数校验：用户名过短返回 400")
    void registerInvalid() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("注册重名：返回 400 用户名已存在")
    void registerDuplicate() throws Exception {
        // 先注册一次
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(0));
        // 同名再注册
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("登录成功：返回 token 与用户信息")
    void loginOk() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("testw3d4"));
    }

    @Test
    @DisplayName("登录失败：密码错误返回统一文案")
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"wrong123\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("/api/user/me 未登录：返回 401")
    void meUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("/api/user/me 带 token：返回当前用户")
    void meOk() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("testw3d4"));
    }

    private String registerAndLogin() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(0));
        String body = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testw3d4\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        // 从 {"code":0,"message":"success","data":{"token":"...","user":{...}}} 里抠 token
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }
}
