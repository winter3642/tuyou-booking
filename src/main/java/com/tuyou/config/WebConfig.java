package com.tuyou.config;

import com.tuyou.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册 JWT 拦截器
 * 放行：健康检查 / 注册登录 / 产品公开查询 / 分类 / 目的地
 * 拦截：/api/** 其余路径（含 /api/admin/** 管理接口，需登录）
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/user/register",
                        "/api/user/login",
                        "/api/products",
                        "/api/products/**",
                        "/api/categories",
                        "/api/destinations"
                );
    }
}
