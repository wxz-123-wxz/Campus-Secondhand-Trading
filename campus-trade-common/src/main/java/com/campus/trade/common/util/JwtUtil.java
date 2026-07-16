package com.campus.trade.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（HS256）。
 * Token 载荷：sub=userId, username=用户名。
 * 网关验证后将 X-User-Id / X-Username 注入下游请求头。
 */
public class JwtUtil {

    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000L; // 24 小时

    private final SecretKey key;

    public JwtUtil(String secret) {
        // 密钥至少 256 bit，建议在配置文件中使用长随机字符串
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 JWT */
    public String generate(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRE_MS))
                .signWith(key)
                .compact();
    }

    /** 解析并校验 JWT；失败抛 JwtException */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从 token 中提取 userId */
    public Long getUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }

    /** 从 token 中提取 username */
    public String getUsername(String token) {
        return parse(token).get("username", String.class);
    }

    /** 简单校验是否有效（签名 + 未过期） */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
