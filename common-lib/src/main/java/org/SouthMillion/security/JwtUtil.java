package org.SouthMillion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(String secretKeyString, long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
        this.expirationMs = expirationMs;
    }

    // Sinh token (chỉ dùng ở user-service)
    public String generateToken(String id, String username, String email) {
        SecretKey key = secretKey;
        return Jwts.builder()
                .setSubject(id)
                .claim("username", username)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    // Lấy userId từ token
    public Long extractUserId(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();
        // Nếu khi tạo JWT bạn setSubject(userId.toString()), trả về Long
        return Long.parseLong(claims.getSubject());
    }

    // Lấy username từ token
    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();
        return claims.get("username", String.class);
    }

        // Kiểm tra token hợp lệ
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey)
                    .parseClaimsJws(token.replace("Bearer ", ""));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
