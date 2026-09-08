package com.tuyou.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 工具类单元测试（纯 JUnit，不依赖 Spring）
 * 验证：签发/解析闭环、防伪、过期拒绝
 */
class JwtUtilTest {

    @Test
    @DisplayName("生成的 token 能被解析，subject 与 username 正确")
    void generateAndParse() {
        String token = JwtUtil.generateToken(1001L, "test01");
        assertNotNull(token);

        Claims claims = JwtUtil.parseToken(token);
        assertEquals("1001", claims.getSubject());
        assertEquals("test01", claims.get("username", String.class));
    }

    @Test
    @DisplayName("篡改 token 必须解析失败（防伪）")
    void tamperedTokenShouldFail() {
        String token = JwtUtil.generateToken(1001L, "test01");
        // 改动签名段最后两位，破坏签名
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(JwtException.class, () -> JwtUtil.parseToken(tampered));
    }

    @Test
    @DisplayName("过期 token 必须被拒绝")
    void expiredTokenShouldFail() throws Exception {
        // 反射读取私有 SECRET，用同一密钥构造一个已过期的 token
        Field field = JwtUtil.class.getDeclaredField("SECRET");
        field.setAccessible(true);
        String secret = (String) field.get(null);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String expired = Jwts.builder()
                .subject("1001")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(key)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> JwtUtil.parseToken(expired));
    }

    @Test
    @DisplayName("null 与空 token 必须抛异常")
    void nullOrEmptyTokenShouldFail() {
        assertThrows(Exception.class, () -> JwtUtil.parseToken(null));
        assertThrows(Exception.class, () -> JwtUtil.parseToken(""));
        assertThrows(Exception.class, () -> JwtUtil.parseToken("not-a-jwt"));
    }
}
