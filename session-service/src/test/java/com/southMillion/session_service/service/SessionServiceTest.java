package com.SouthMillion.session_service.service;

import com.SouthMillion.session_service.service.client.UserFeignClient;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.SouthMillion.dto.session.LoginDTOs;
import org.SouthMillion.dto.user.VerifyResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService Tests")
class SessionServiceTest {

    @Mock private UserFeignClient              userFeign;
    @Mock private UserStatusService            userStatus;
    @Mock private TokenService                 tokenSvc;
    @Mock private StringRedisTemplate          redis;
    @Mock private RateLimitService             rateLimit;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private SessionService sessionService;

    // HS256 secret >= 32 bytes, plain-text (resolveSecret falls back to UTF-8)
    private static final String JWT_SECRET = "test-secret-32-bytes-for-hs256!!";
    private static final String JWT_ISSUER = "game-server";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sessionService, "issuer",    JWT_ISSUER);
        ReflectionTestUtils.setField(sessionService, "jwtSecret", JWT_SECRET);
    }

    /** Creates a real, signed JWT using the same secret/issuer as the service. */
    private String makeToken(String userId, String sid, long ttlSeconds) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(JWT_ISSUER)
                .subject(userId)
                .expirationTime(Date.from(Instant.now().plusSeconds(ttlSeconds)))
                .issueTime(new Date())
                .claim("typ", "access")
                .claim("sid", sid)
                .claim("account", "testUser")
                .build();
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    // =========================================================
    // login()
    // =========================================================
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("TC-SESS-001 [N] Request null – nem IllegalArgumentException")
        void login_nullRequest_throws() {
            assertThatThrownBy(() -> sessionService.login(null, "127.0.0.1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid_request");
        }

        @Test
        @DisplayName("TC-SESS-002 [N] Sai thong tin dang nhap – nem bad_credentials")
        void login_badCredentials_throws() {
            VerifyResp resp = mock(VerifyResp.class);
            given(resp.isOk()).willReturn(false);
            given(userFeign.verifyPassword(any())).willReturn(resp);

            LoginDTOs.LoginReq req = new LoginDTOs.LoginReq();
            req.setUsername("user"); req.setPassword("wrong");
            assertThatThrownBy(() -> sessionService.login(req, "127.0.0.1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("bad_credentials");
        }

        @Test
        @DisplayName("TC-SESS-003 [N] User bi khoa – nem user_inactive")
        void login_userInactive_throws() {
            VerifyResp resp = mock(VerifyResp.class);
            given(resp.isOk()).willReturn(true);
            given(resp.getUserId()).willReturn("u1");
            given(resp.getUsername()).willReturn("user");
            given(userFeign.verifyPassword(any())).willReturn(resp);
            given(userStatus.isActive("u1")).willReturn(false);

            LoginDTOs.LoginReq req = new LoginDTOs.LoginReq();
            req.setUsername("user"); req.setPassword("pass");
            assertThatThrownBy(() -> sessionService.login(req, "127.0.0.1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("user_inactive");
        }

        @Test
        @DisplayName("TC-SESS-004 [P] Dang nhap thanh cong – tra ve LoginResp")
        void login_success_returnsResp() {
            VerifyResp resp = mock(VerifyResp.class);
            given(resp.isOk()).willReturn(true);
            given(resp.getUserId()).willReturn("u1");
            given(resp.getUsername()).willReturn("user");
            given(userFeign.verifyPassword(any())).willReturn(resp);
            given(userStatus.isActive("u1")).willReturn(true);
            LoginDTOs.LoginResp expected = LoginDTOs.LoginResp.builder()
                    .userId("u1").account("user").build();
            given(tokenSvc.issueTokens("u1", "user", "dev1")).willReturn(expected);

            LoginDTOs.LoginReq req = new LoginDTOs.LoginReq();
            req.setUsername("user"); req.setPassword("pass"); req.setDeviceId("dev1");
            LoginDTOs.LoginResp result = sessionService.login(req, "127.0.0.1");

            assertThat(result.getUserId()).isEqualTo("u1");
            then(rateLimit).should().checkLogin("127.0.0.1", "user");
        }
    }

    // =========================================================
    // refresh()
    // =========================================================
    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("TC-SESS-005 [N] Request null – nem refresh_token_missing")
        void refresh_nullRequest_throws() {
            assertThatThrownBy(() -> sessionService.refresh(null, "u1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("refresh_token_missing");
        }

        @Test
        @DisplayName("TC-SESS-006 [N] Token rong – nem refresh_token_missing")
        void refresh_blankToken_throws() {
            LoginDTOs.RefreshReq req = new LoginDTOs.RefreshReq();
            req.setRefreshToken("");
            assertThatThrownBy(() -> sessionService.refresh(req, "u1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("refresh_token_missing");
        }

        @Test
        @DisplayName("TC-SESS-007 [P] Refresh thanh cong – tra ve RefreshResp")
        void refresh_success_returnsResp() {
            LoginDTOs.RefreshReq req = new LoginDTOs.RefreshReq();
            req.setRefreshToken("validToken");
            LoginDTOs.RefreshResp expected = LoginDTOs.RefreshResp.builder()
                    .userId("u1").accessToken("newAccess").build();
            given(tokenSvc.refresh("validToken")).willReturn(expected);

            LoginDTOs.RefreshResp result = sessionService.refresh(req, "u1");

            assertThat(result.getAccessToken()).isEqualTo("newAccess");
            then(rateLimit).should().checkRefresh("u1");
        }
    }

    // =========================================================
    // heartbeat()
    // =========================================================
    @Nested
    @DisplayName("heartbeat()")
    class Heartbeat {

        @Test
        @DisplayName("TC-SESS-008 [N] Token khong hop le – nem invalid_token")
        void heartbeat_invalidToken_throws() {
            assertThatThrownBy(() -> sessionService.heartbeat("not-a-jwt"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid_token");
        }

        @Test
        @DisplayName("TC-SESS-009 [P] Token hop le – goi tokenSvc.heartbeat")
        void heartbeat_validToken_callsHeartbeat() throws Exception {
            String token = makeToken("u1", "sess1", 900);

            sessionService.heartbeat(token);

            then(tokenSvc).should().heartbeat("sess1");
        }
    }

    // =========================================================
    // logout()
    // =========================================================
    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("TC-SESS-010 [N] Token khong hop le – nem invalid_token")
        void logout_invalidToken_throws() {
            assertThatThrownBy(() -> sessionService.logout("garbage"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid_token");
        }

        @Test
        @DisplayName("TC-SESS-011 [P] Token hop le – goi tokenSvc.logoutByAccess")
        void logout_validToken_callsLogout() throws Exception {
            String token = makeToken("u1", "sess1", 900);

            sessionService.logout(token);

            then(tokenSvc).should().logoutByAccess(eq(token), eq("sess1"));
        }
    }

    // =========================================================
    // introspect()
    // =========================================================
    @Nested
    @DisplayName("introspect()")
    class Introspect {

        @Test
        @DisplayName("TC-SESS-012 [N] Token khong hop le – tra ve active=false")
        void introspect_invalidToken_returnsInactive() {
            LoginDTOs.IntrospectResp result = sessionService.introspect("bad-token");
            assertThat(result.isActive()).isFalse();
            assertThat(result.getReason()).isEqualTo("invalid_token");
        }

        @Test
        @DisplayName("TC-SESS-013 [N] Session bi thu hoi – tra ve revoked")
        void introspect_sessionRevoked_returnsRevoked() throws Exception {
            String token = makeToken("u1", "sess1", 900);
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get("sess:sess1")).willReturn(null);

            LoginDTOs.IntrospectResp result = sessionService.introspect(token);

            assertThat(result.isActive()).isFalse();
            assertThat(result.getReason()).isEqualTo("revoked");
        }

        @Test
        @DisplayName("TC-SESS-014 [N] User bi khoa – tra ve user_inactive")
        void introspect_userInactive_returnsInactive() throws Exception {
            String token = makeToken("u1", "sess1", 900);
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get("sess:sess1")).willReturn("{...}");
            given(userStatus.isActive("u1")).willReturn(false);

            LoginDTOs.IntrospectResp result = sessionService.introspect(token);

            assertThat(result.isActive()).isFalse();
            assertThat(result.getReason()).isEqualTo("user_inactive");
        }

        @Test
        @DisplayName("TC-SESS-015 [P] Token va session hop le – tra ve active=true")
        void introspect_valid_returnsActive() throws Exception {
            String token = makeToken("u1", "sess1", 900);
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get("sess:sess1")).willReturn("{...}");
            given(userStatus.isActive("u1")).willReturn(true);

            LoginDTOs.IntrospectResp result = sessionService.introspect(token);

            assertThat(result.isActive()).isTrue();
            assertThat(result.getUserId()).isEqualTo("u1");
            assertThat(result.getSessionId()).isEqualTo("sess1");
        }
    }

    // =========================================================
    // resolveSecret() & hexToBytes() – package-private static helpers
    // =========================================================
    @Nested
    @DisplayName("resolveSecret() static helper")
    class ResolveSecret {

        @Test
        @DisplayName("TC-SESS-016 [N] Input null – tra ve mang rong")
        void resolveSecret_null_returnsEmpty() {
            assertThat(SessionService.resolveSecret(null)).isEmpty();
        }

        @Test
        @DisplayName("TC-SESS-017 [P] Chuoi HEX – giai ma thanh bytes")
        void resolveSecret_hex_decodesCorrectly() {
            // "deadbeef" = 4 bytes
            byte[] result = SessionService.resolveSecret("deadbeef");
            assertThat(result).containsExactly((byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef);
        }

        @Test
        @DisplayName("TC-SESS-018 [P] Chuoi UTF-8 binh thuong – giu nguyen bytes")
        void resolveSecret_utf8_returnsUtf8Bytes() {
            byte[] result = SessionService.resolveSecret("hello");
            assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("hello");
        }
    }
}
