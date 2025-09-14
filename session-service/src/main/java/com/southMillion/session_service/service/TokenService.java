package com.southMillion.session_service.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LoginDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redis;

    @Value("${jwt.issuer}")                 private String issuer;
    @Value("${jwt.secret}")                 private String jwtSecret;         // chuỗi HEX/Base64/Plaintext đều OK
    @Value("${jwt.access-ttl-seconds:900}") private long accessTtl;
    @Value("${jwt.refresh-ttl-seconds:604800}") private long refreshTtl;
    @Value("${jwt.rotate-refresh:true}")    private boolean rotateRefresh;

    // ===== API =====

    public LoginDTOs.LoginResp issueTokens(String userId, String account, String deviceId) {
        String sid = genSessionId();

        // Lưu session
        String sessKey = keySess(sid);
        String sessJson = "{\"userId\":\"" + esc(userId) + "\",\"account\":\"" + esc(account) + "\",\"deviceId\":\"" + esc(deviceId) + "\"}";
        redis.opsForValue().set(sessKey, sessJson, refreshTtl, TimeUnit.SECONDS);

        // Refresh token random
        String refresh = randomBase64Url(32);
        bindRefreshToSid(refresh, sid);

        // Access token (JWT)
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtl);
        String access = signAccessToken(userId, account, deviceId, sid, now, exp);

        return LoginDTOs.LoginResp.builder()
                .userId(userId)
                .account(account)
                .sessionId(sid)
                .accessToken(access)
                .expiresAt(exp.getEpochSecond())
                .refreshToken(refresh)
                .tokenType("Bearer")
                .build();
    }

    public LoginDTOs.RefreshResp refresh(String refreshToken) {
        String sid = redis.opsForValue().get(keyRt(refreshToken));
        if (sid == null) {
            if (Boolean.TRUE.equals(redis.hasKey(keyRevokedRt(refreshToken)))) {
                throw new IllegalStateException("refresh_revoked");
            }
            throw new IllegalStateException("refresh_invalid");
        }

        String sessJson = redis.opsForValue().get(keySess(sid));
        if (sessJson == null) {
            throw new IllegalStateException("session_expired");
        }
        Map<String,String> sess = Json.minParse(sessJson);
        String userId  = sess.get("userId");
        String account = sess.get("account");
        String deviceId = sess.get("deviceId");

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtl);
        String access = signAccessToken(userId, account, deviceId, sid, now, exp);

        String newRefresh = refreshToken;
        if (rotateRefresh) {
            redis.delete(keyRt(refreshToken));
            redis.opsForValue().set(keyRevokedRt(refreshToken), "1", refreshTtl, TimeUnit.SECONDS);
            newRefresh = randomBase64Url(32);
            bindRefreshToSid(newRefresh, sid);
        }

        return LoginDTOs.RefreshResp.builder()
                .userId(userId)
                .account(account)
                .sessionId(sid)
                .accessToken(access)
                .expiresAt(exp.getEpochSecond())
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .build();
    }

    public void logoutByAccess(String accessToken, String sid) {
        String sidSetKey = keyRtBySid(sid);
        Set<String> rts = redis.opsForSet().members(sidSetKey);
        if (rts != null) {
            for (String rt : rts) {
                redis.delete(keyRt(rt));
                redis.opsForValue().set(keyRevokedRt(rt), "1", refreshTtl, TimeUnit.SECONDS);
            }
        }
        redis.delete(sidSetKey);
        redis.delete(keySess(sid));
        // Không cần blacklist access: session bị xoá → introspect sẽ fail.
    }

    public void heartbeat(String sid) {
        Long ttl = redis.getExpire(keySess(sid));
        if (ttl == null || ttl <= 0) throw new IllegalStateException("session_expired");

        String sessJson = redis.opsForValue().get(keySess(sid));
        if (sessJson == null) throw new IllegalStateException("session_expired");

        redis.opsForValue().set(keySess(sid), sessJson, refreshTtl, TimeUnit.SECONDS);
        redis.opsForValue().set(keyHb(sid), String.valueOf(Instant.now().getEpochSecond()), refreshTtl, TimeUnit.SECONDS);
    }

    // ===== JWT ký bằng Nimbus HS256 =====

    private String signAccessToken(String userId, String account, String deviceId, String sid, Instant iat, Instant exp) {
        try {
            byte[] key = resolveSecret(jwtSecret);
            if (key.length < 32) { // HS256 yêu cầu >= 256-bit
                throw new IllegalStateException("JWT secret too short for HS256 (need >= 32 bytes)");
            }

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .issueTime(Date.from(iat))
                    .expirationTime(Date.from(exp))
                    .subject(userId)
                    .claim("typ", "access")
                    .claim("sid", sid)
                    .claim("account", account)
                    .claim("deviceId", deviceId)
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new MACSigner(key));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("sign_jwt_failed", e);
        }
    }

    // ===== Helpers =====

    private static String genSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String randomBase64Url(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private void bindRefreshToSid(String refresh, String sid) {
        redis.opsForValue().set(keyRt(refresh), sid, refreshTtl, TimeUnit.SECONDS);
        redis.opsForSet().add(keyRtBySid(sid), refresh);
        redis.expire(keyRtBySid(sid), refreshTtl, TimeUnit.SECONDS);
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String keySess(String sid)       { return "sess:" + sid; }
    private static String keyRt(String rt)          { return "rt:" + rt; }
    private static String keyRevokedRt(String rt)   { return "revoked:rt:" + rt; }
    private static String keyRtBySid(String sid)    { return "rtbySid:" + sid; }
    private static String keyHb(String sid)         { return "hb:" + sid; }

    /** Mini JSON helper (đủ dùng vì value không chứa dấu , :) */
    static class Json {
        static Map<String,String> minParse(String json) {
            return Arrays.stream(json.replaceAll("[{}\"]","").split(","))
                    .map(kv -> kv.split(":",2))
                    .filter(kv -> kv.length==2)
                    .collect(java.util.stream.Collectors.toMap(kv -> kv[0], kv -> kv[1]));
        }
    }

    // ===== Secret resolver: nhận HEX / Base64URL / Base64 / UTF-8 =====

    private static final Pattern HEX = Pattern.compile("^[0-9a-fA-F]{32,}$"); // >=16 bytes (hex)
    private static final Pattern B64URL = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    static byte[] resolveSecret(String raw) {
        if (raw == null) return new byte[0];
        String s = raw.trim();

        // HEX?
        if (HEX.matcher(s).matches() && (s.length() % 2 == 0)) {
            return hexToBytes(s);
        }

        // Base64URL?
        if (B64URL.matcher(s).matches()) {
            try { return Base64.getUrlDecoder().decode(s); } catch (IllegalArgumentException ignore) {}
        }

        // Base64 chuẩn?
        try { return Base64.getDecoder().decode(s); } catch (IllegalArgumentException ignore) {}

        // Fallback: bytes UTF-8 của chuỗi
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    +  Character.digit(hex.charAt(i+1), 16));
        }
        return out;
    }
}