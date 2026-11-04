package com.codeagent.utils;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.UserContext;
import com.codeagent.entity.SysUser;
import com.codeagent.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expireSeconds;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expire-seconds:7200}") long expireSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度不能少于 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireSeconds = expireSeconds;
    }

    public String createToken(SysUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLE, user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public UserContext.UserInfo parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get(CLAIM_USERNAME, String.class);
            String role = claims.get(CLAIM_ROLE, String.class);
            if (username == null || role == null) {
                throw new IllegalArgumentException("token 缺少必要 claim");
            }
            return new UserContext.UserInfo(userId, username, role);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
