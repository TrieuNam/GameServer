package com.southMillion.session_service.service;


import com.southMillion.session_service.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.TokenPair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final JwtService jwt;
    private final SessionStore store;
    private final JwtProperties props;

    public TokenPair issue(String userId, String username, String ip, String ua) {
        String sid = UUID.randomUUID().toString();
        String access = jwt.createAccessToken(userId, username, sid, Map.of());
        String refresh = jwt.createRefreshToken(userId, username, sid);
        long now = Instant.now().getEpochSecond();
        long accExp = now + props.getAccessTtlSeconds();
        long refExp = now + props.getRefreshTtlSeconds();
        store.saveSession(sid, userId, username, refresh, props.getRefreshTtlSeconds(), ip, ua);
        return TokenPair.builder()
                .accessToken(access).accessExpiresAt(accExp)
                .refreshToken(refresh).refreshExpiresAt(refExp)
                .sessionId(sid)
                .build();
    }

    public TokenPair refresh(String refreshToken) {
        Jws<io.jsonwebtoken.Claims> j = jwt.parse(refreshToken);
        Claims c = j.getBody();
        if (!"refresh".equals(c.get("type"))) throw new IllegalArgumentException("invalid token type");
        String userId = c.getSubject();
        String username = String.valueOf(c.get("uname"));
        String sid = String.valueOf(c.get("sid"));
        if (!store.verifyRefresh(sid, refreshToken)) {
            throw new IllegalStateException("refresh revoked or rotated");
        }
        String newAccess = jwt.createAccessToken(userId, username, sid, Map.of());
        long now = Instant.now().getEpochSecond();
        long accExp = now + props.getAccessTtlSeconds();
        String newRefresh = refreshToken;
        long refExp = c.getExpiration().toInstant().getEpochSecond();
        if (props.isRotateRefresh()) {
            newRefresh = jwt.createRefreshToken(userId, username, sid);
            refExp = now + props.getRefreshTtlSeconds();
            store.updateRefresh(sid, newRefresh, props.getRefreshTtlSeconds());
        }
        return TokenPair.builder()
                .accessToken(newAccess).accessExpiresAt(accExp)
                .refreshToken(newRefresh).refreshExpiresAt(refExp)
                .sessionId(sid)
                .build();
    }

    public void logout(String sessionId) {
        store.delete(sessionId);
    }
}