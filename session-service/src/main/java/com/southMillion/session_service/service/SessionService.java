package com.southMillion.session_service.service;



import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.southMillion.session_service.service.client.UserFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LoginDTOs;
import org.SouthMillion.dto.user.VerifyReq;
import org.SouthMillion.dto.user.VerifyResp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserFeignClient userFeign;
    private final UserStatusService userStatus;
    private final TokenService tokenSvc;
    private final StringRedisTemplate redis;
    private final RateLimitService rateLimit;

    @Value("${jwt.issuer}") private String issuer;
    @Value("${jwt.secret}") private String jwtSecret;

    // ===== API =====

    public LoginDTOs.LoginResp login(LoginDTOs.LoginReq req, String ip) {
        if (req == null) throw new IllegalArgumentException("invalid_request");
        // Rate-limit theo IP + username
        rateLimit.checkLogin(ip, req.getUsername());

        // Verify username/password qua user-service
        VerifyReq v = VerifyReq.builder()
                .username(req.getUsername())
                .password(req.getPassword())
                .build();
        VerifyResp ok = userFeign.verifyPassword(v);
        if (ok == null || !ok.isOk()) {
            throw new IllegalStateException("bad_credentials");
        }

        // Kiểm tra user active
        Boolean active = userStatus.isActive(ok.getUserId());
        if (active == null || !active) {
            throw new IllegalStateException("user_inactive");
        }

        // Cấp token + tạo session
        return tokenSvc.issueTokens(ok.getUserId(), ok.getUsername(), req.getDeviceId());
    }

    public LoginDTOs.RefreshResp refresh(LoginDTOs.RefreshReq req, String userIdForRl) {
        if (req == null || req.getRefreshToken() == null || req.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("refresh_token_missing");
        }
        // Rate-limit theo user
        rateLimit.checkRefresh(userIdForRl == null ? "unknown" : userIdForRl);
        return tokenSvc.refresh(req.getRefreshToken());
    }

    public void heartbeat(String accessToken) {
        DecodedAccess t = requireValidAccess(accessToken);
        tokenSvc.heartbeat(t.sid());
    }

    public void logout(String accessToken) {
        DecodedAccess t = requireValidAccess(accessToken);
        tokenSvc.logoutByAccess(accessToken, t.sid());
    }

    public LoginDTOs.IntrospectResp introspect(String accessToken) {
        DecodedAccess t = decodeAndVerify(accessToken);
        if (t == null) {
            return LoginDTOs.IntrospectResp.builder().active(false).reason("invalid_token").build();
        }

        // session còn tồn tại?
        String sess = redis.opsForValue().get("sess:" + t.sid());
        if (sess == null) {
            return LoginDTOs.IntrospectResp.builder().active(false).reason("revoked").build();
        }

        // user còn active?
        Boolean active = userStatus.isActive(t.userId());
        if (active == null || !active) {
            return LoginDTOs.IntrospectResp.builder().active(false).reason("user_inactive").build();
        }

        return LoginDTOs.IntrospectResp.builder()
                .active(true)
                .userId(t.userId())
                .account(t.account())
                .sessionId(t.sid())
                .exp(t.exp() == null ? 0L : t.exp().getEpochSecond())
                .build();
    }

    // ===== JWT verify (Nimbus HS256) =====

    private DecodedAccess requireValidAccess(String token) {
        DecodedAccess t = decodeAndVerify(token);
        if (t == null) throw new IllegalArgumentException("invalid_token");
        return t;
    }

    /**
     * Trả về null nếu token không hợp lệ/expired/sai issuer/alg/typ.
     */
    private DecodedAccess decodeAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            // Kiểm tra thuật toán
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
                return null;
            }

            // Verify chữ ký
            byte[] key = resolveSecret(jwtSecret);
            if (key.length < 32) return null; // HS256 cần >= 32 bytes
            if (!jwt.verify(new MACVerifier(key))) return null;

            var claims = jwt.getJWTClaimsSet();

            // iss
            if (claims.getIssuer() == null || !claims.getIssuer().equals(issuer)) return null;
            // exp
            Date exp = claims.getExpirationTime();
            if (exp == null || Instant.now().isAfter(exp.toInstant())) return null;
            // typ
            Object typ = claims.getClaim("typ");
            if (!(typ instanceof String) || !"access".equals(typ)) return null;

            String userId  = claims.getSubject();
            String sid     = optStr(claims.getClaim("sid"));
            String account = optStr(claims.getClaim("account"));
            String device  = optStr(claims.getClaim("deviceId"));

            if (userId == null || sid == null) return null;

            return new DecodedAccess(userId, account, device, sid, exp.toInstant());
        } catch (Exception e) {
            return null;
        }
    }

    private static String optStr(Object o) { return o == null ? null : String.valueOf(o); }

    // ===== Secret resolver: nhận HEX / Base64URL / Base64 / UTF-8 (trùng logic với TokenService) =====
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

        // Fallback: UTF-8
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

    // ===== DTO nội bộ cho claim đã verify =====
    record DecodedAccess(String userId, String account, String deviceId, String sid, Instant exp) {}
}