package com.autotesthub.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Base64 编码的密钥，开发环境使用
    private static final String SECRET_KEY =
            "YXV0b3Rlc3RodWItZGV2ZWxvcG1lbnQtc2VjcmV0LWtleS0yMDI2";

    // Token 有效期：2 小时
    private static final long EXPIRATION_TIME =
            1000 * 60 * 60 * 2;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(Long userId, String username) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + EXPIRATION_TIME
        );

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     */
    public Claims parseToken(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}