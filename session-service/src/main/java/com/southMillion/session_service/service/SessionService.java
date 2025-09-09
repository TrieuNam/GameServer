package com.southMillion.session_service.service;



import com.southMillion.session_service.service.client.UserFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LoginDTOs;
import org.SouthMillion.dto.user.VerifyReq;
import org.SouthMillion.dto.user.VerifyResp;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserFeignClient userAuth;
    private final TokenService tokenService;
    private final JwtDecoder jwtDecoder;
    private final StringRedisTemplate redis;
    private final UserStatusService userStatus;

    public LoginDTOs.TokenPair login(String username, String password) {
        VerifyResp resp = userAuth.verifyPassword(new VerifyReq(username, password));
        if (resp == null || !resp.isOk()) return null;

        // phát token theo userId + account (KHÔNG dùng password)
        String account = resp.getAccount() != null ? resp.getAccount() : resp.getUsername();
        return tokenService.issue(resp.getUserId(), account);
    }

    public LoginDTOs.TokenPair refresh(String refreshToken) {
        String key = "rt:" + refreshToken;
        if (!Boolean.TRUE.equals(redis.hasKey(key))) return null;

        Map<Object,Object> map = redis.opsForHash().entries(key);
        String uid = (String) map.get("uid");
        String acc = (String) map.get("acc");

        // kiểm tra user active (có cache 60s)
        Boolean active = userStatus.isActive(uid);
        if (active == null || !active) return null;

        // rotate refresh token: xoá token cũ, phát token mới
        redis.delete(key);
        return tokenService.issue(uid, acc);
    }

    public LoginDTOs.IntrospectResp introspect(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            String userId = jwt.getSubject();
            String account = jwt.getClaimAsString("acc");
            String sid = jwt.getClaimAsString("sid");
            Instant exp = jwt.getExpiresAt();

            if (sid != null && tokenService.isSessionRevoked(sid)) {
                return LoginDTOs.IntrospectResp.builder().active(false).reason("revoked").build();
            }

            Boolean active = userStatus.isActive(userId);
            if (active == null || !active) {
                return LoginDTOs.IntrospectResp.builder().active(false).reason("user_inactive").build();
            }

            return LoginDTOs.IntrospectResp.builder()
                    .active(true).userId(userId).account(account).sessionId(sid).exp(exp).build();
        } catch (JwtException e) {
            return LoginDTOs.IntrospectResp.builder().active(false).reason("invalid_token").build();
        }
    }
}