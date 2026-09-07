package com.tuyou.controller;

import com.tuyou.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口：验证应用启动成功、统一返回体生效
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Result<String> health() {
        return Result.ok("tuyou-booking is running");
    }
}
