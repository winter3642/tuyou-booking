package com.tuyou.interceptor;

import com.tuyou.common.BizException;
import com.tuyou.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JWT 拦截器单元测试（W3D4 补全）
 * 覆盖：OPTIONS 放行 / 缺失与伪造 token 401 / 合法 token 放行并写入 ThreadLocal / 结束清理
 */
class JwtInterceptorTest {

    private final JwtInterceptor interceptor = new JwtInterceptor();

    @Test
    @DisplayName("OPTIONS 预检请求直接放行（CORS）")
    void preflightPass() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("OPTIONS");
        assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));
    }

    @Test
    @DisplayName("无 Authorization 头：抛 401")
    void noHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("Authorization 头格式错误（非 Bearer）：抛 401")
    void wrongScheme() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        BizException ex = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("伪造 token：解析失败抛 401，不写入 ThreadLocal")
    void forgedToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer not.a.jwt");

        BizException ex = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));
        assertEquals(401, ex.getCode());
        assertNull(LoginUserHolder.get());
    }

    @Test
    @DisplayName("合法 token：放行并把 userId/username 写入 ThreadLocal")
    void validToken() {
        String token = JwtUtil.generateToken(42L, "test01");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));
        assertEquals(42L, LoginUserHolder.get());
        assertEquals("test01", LoginUserHolder.getUsername());
        LoginUserHolder.clear(); // 单测无请求生命周期，手动清理
    }

    @Test
    @DisplayName("请求结束 afterCompletion：清理 ThreadLocal 防内存泄漏")
    void afterCompletionClears() {
        LoginUserHolder.set(1L, "test01");
        interceptor.afterCompletion(mock(HttpServletRequest.class), mock(HttpServletResponse.class),
                new Object(), null);
        assertNull(LoginUserHolder.get());
        assertNull(LoginUserHolder.getUsername());
    }
}
