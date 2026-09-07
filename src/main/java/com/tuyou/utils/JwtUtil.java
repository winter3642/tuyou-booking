package com.tuyou.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（jjwt 0.12 新 API）
 * 注意：0.12 解析用 verifyWith + parseSignedClaims，网上 0.11 的 parseClaimsJws 写法会报错
 */
public class JwtUtil {

    /** 生产环境务必放到配置/环境变量，这里仅演示 */
    private static final String SECRET = "tuyou-booking-secret-key-please-change-in-production-2026";
    private static final long EXPIRE_MS = 7 * 24 * 3600_000L; // 7 天

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** 签发 token */
    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(KEY)
                .compact();
    }

    /** 解析 token，成功返回 Claims；过期/伪造会抛异常 */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
