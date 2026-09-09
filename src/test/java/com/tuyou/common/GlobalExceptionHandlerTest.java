package com.tuyou.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 全局异常处理器测试（W3D4 补全）
 * 覆盖三类异常：业务异常 / 参数校验异常（有/无字段错误）/ 兜底系统异常
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("业务异常：透传错误码与消息")
    void handleBiz() {
        Result<Void> r = handler.handleBiz(new BizException(404, "产品不存在"));
        assertEquals(404, r.code());
        assertEquals("产品不存在", r.message());
    }

    @Test
    @DisplayName("参数校验异常：返回第一个字段错误信息")
    void handleValidWithFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("registerDTO", "username", "用户名长度 3-20");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        Result<Void> r = handler.handleValid(ex);
        assertEquals(400, r.code());
        assertEquals("username: 用户名长度 3-20", r.message());
    }

    @Test
    @DisplayName("参数校验异常：无字段错误时返回兜底文案")
    void handleValidWithoutFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(null);

        Result<Void> r = handler.handleValid(ex);
        assertEquals(400, r.code());
        assertEquals("参数错误", r.message());
    }

    @Test
    @DisplayName("兜底异常：返回 500 系统繁忙，不泄露内部细节")
    void handleAll() {
        Result<Void> r = handler.handleAll(new RuntimeException("内部错误"));
        assertEquals(500, r.code());
        assertEquals("系统繁忙", r.message());
    }
}
