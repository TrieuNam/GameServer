package com.southMillion.session_service.service;

import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LoginDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String RT_KEY_PREFIX = "rt:";                   // khoá lưu refresh token
    private static final String SID_BLACKLIST_PREFIX = "sid:blacklist:"; // khoá blacklist theo session

    private final JwtEncoder jwtEncoder;
    private final StringRedisTemplate redis;

    @Value("${security.jwt.access-ttl-sec:900}")       // 15 phút mặc định
    private long accessTtlSec;

    @Value("${security.jwt.refresh-ttl-sec:2592000}")  // 30 ngày mặc định
    private long refreshTtlSec;

    @Value("${security.jwt.issuer:SouthMillion}")
    private String issuer;

    /**
     * Phát cặp token:
     * - access token (JWT HS256) chứa claim acc, sid
     * - refresh token (opaque) lưu trong Redis
     */
    public LoginDTOs.TokenPair issue(String userId, String account) {
        String sessionId = genSessionId();
        Instant now = Instant.now();
        Instant accessExp = now.plusSeconds(accessTtlSec);
        Instant refreshExp = now.plusSeconds(refreshTtlSec);

        // ===== Access JWT
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(accessExp)
                .subject(userId)
                .claim("acc", account)
                .claim("sid", sessionId)
                .build();

        String access = jwtEncoder
                .encode(JwtEncoderParameters.from(jwsHeader, claims))
                .getTokenValue();

        // ===== Refresh (opaque) + Redis
        String refresh = "rt_" + randomBase64Url(32);
        String key = RT_KEY_PREFIX + refresh;
        redis.opsForHash().putAll(key, Map.of(
                "uid", userId,
                "acc", account,
                "sid", sessionId,
                "exp", String.valueOf(refreshExp.getEpochSecond())
        ));
        redis.expire(key, refreshTtlSec, TimeUnit.SECONDS);

        return LoginDTOs.TokenPair.builder()
                .accessToken(access)
                .accessExpiresAt(accessExp.getEpochSecond())
                .refreshToken(refresh)
                .refreshExpiresAt(refreshExp.getEpochSecond())
                .sessionId(sessionId)
                .build();
    }

    /** Đưa session vào blacklist đến hết hạn refresh token */
    public void revokeBySession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        redis.opsForValue().set(SID_BLACKLIST_PREFIX + sessionId, "1", refreshTtlSec, TimeUnit.SECONDS);
    }

    /** Kiểm tra session đã bị revoke chưa */
    public boolean isSessionRevoked(String sessionId) {
        return sessionId != null && Boolean.TRUE.equals(redis.hasKey(SID_BLACKLIST_PREFIX + sessionId));
    }

    /** (Tuỳ chọn) Revoke refresh token cụ thể – dùng khi logout 1 thiết bị */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        redis.delete(RT_KEY_PREFIX + refreshToken);
    }

    // ===== Helpers

    private static String genSessionId() {
        // 32 hex chars (UUID không dấu '-')
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String randomBase64Url(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}