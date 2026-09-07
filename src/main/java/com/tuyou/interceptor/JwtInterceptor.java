package com.tuyou.interceptor;

import com.tuyou.common.BizException;
import com.tuyou.common.ResultCode;
import com.tuyou.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器：从 Authorization: Bearer xxx 取 token，校验后把 userId 放入 ThreadLocal
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        try {
            String token = auth.substring(7);
            Claims claims = JwtUtil.parseToken(token);
            LoginUserHolder.set(Long.valueOf(claims.getSubject()));
            return true;
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，防止线程池复用导致串号/内存泄漏（面试常问）
        LoginUserHolder.clear();
    }
}
