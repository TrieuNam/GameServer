package com.southMillion.session_service.service;

import com.southMillion.session_service.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties props;

    private Key key() {
        byte[] k = Decoders.BASE64.decode(Base64.getEncoder().encodeToString(props.getSecret().getBytes()));
        return Keys.hmacShaKeyFor(k);
    }

    public String createAccessToken(String userId, String username, String sessionId, Map<String,Object> extraClaims) {
        long now = Instant.now().getEpochSecond();
        long exp = now + props.getAccessTtlSeconds();
        JwtBuilder b = Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject(userId)
                .setAudience("game-client")
                .setIssuedAt(new Date(now*1000))
                .setExpiration(new Date(exp*1000))
                .claim("uname", username)
                .claim("sid", sessionId)
                .claim("type","access");
        if (extraClaims != null) extraClaims.forEach(b::claim);
        return b.signWith(key(), SignatureAlgorithm.HS256).compact();
    }

    public String createRefreshToken(String userId, String username, String sessionId) {
        long now = Instant.now().getEpochSecond();
        long exp = now + props.getRefreshTtlSeconds();
        return Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject(userId)
                .setIssuedAt(new Date(now*1000))
                .setExpiration(new Date(exp*1000))
                .claim("uname", username)
                .claim("sid", sessionId)
                .claim("type","refresh")
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
    }
}