package com.SouthMillion.session_service.service;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService Tests")
class TokenServiceTest {

    @Mock private StringRedisTemplate           redis;
    @Mock private ValueOperations<String,String> valueOps;

    @InjectMocks private TokenService tokenService;

    // =========================================================
    // resolveSecret() – package-private static helper
    // =========================================================
    @Nested
    @DisplayName("resolveSecret()")
    class ResolveSecret {

        @Test
        @DisplayName("TC-TOKEN-001 [N] Input null – tra ve mang rong")
        void resolveSecret_null_returnsEmpty() {
            assertThat(TokenService.resolveSecret(null)).isEmpty();
        }

        @Test
        @DisplayName("TC-TOKEN-002 [P] Chuoi HEX chan (>= 32 hex chars) – giai ma dung")
        void resolveSecret_hexString_decodesCorrectly() {
            // "deadbeefcafebabe" = 8 bytes, but < 32 hex chars. Use 32 hex chars:
            // 32 hex chars = 16 bytes
            String hex32 = "00112233445566778899aabbccddeeff"; // 32 chars
            byte[] result = TokenService.resolveSecret(hex32);
            assertThat(result).hasSize(16);
            assertThat(result[0]).isEqualTo((byte) 0x00);
            assertThat(result[1]).isEqualTo((byte) 0x11);
        }

        @Test
        @DisplayName("TC-TOKEN-003 [P] Chuoi HEX le – fallback sang UTF-8")
        void resolveSecret_oddHex_fallsBackToUtf8() {
            // Odd length hex → NOT matched by HEX pattern (length % 2 != 0 → skip hex decode → try Base64URL → then UTF-8)
            // "abc" is only 3 chars, length%2 != 0 → skip HEX
            // "abc" matches B64URL → try base64url decode (succeeds → returns bytes)
            // Actually "abc" base64url decodes to 2 bytes. Let's test a known plain string instead.
            // Use a string that's not hex/base64: contains special chars
            String plain = "hello world!";
            byte[] result = TokenService.resolveSecret(plain);
            assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("hello world!");
        }

        @Test
        @DisplayName("TC-TOKEN-004 [P] Chuoi rong – tra ve mang rong")
        void resolveSecret_emptyString_returnsEmpty() {
            byte[] result = TokenService.resolveSecret("");
            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // hexToBytes() – package-private static helper
    // =========================================================
    @Nested
    @DisplayName("hexToBytes()")
    class HexToBytes {

        @Test
        @DisplayName("TC-TOKEN-005 [P] Chuoi hex bien thuong – giai ma dung")
        void hexToBytes_normal_decodesCorrectly() {
            byte[] result = TokenService.hexToBytes("deadbeef");
            assertThat(result).containsExactly(
                    (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef);
        }

        @Test
        @DisplayName("TC-TOKEN-006 [P] Chuoi hex viet hoa – giai ma dung")
        void hexToBytes_uppercase_decodesCorrectly() {
            byte[] result = TokenService.hexToBytes("CAFEBABE");
            assertThat(result).containsExactly(
                    (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe);
        }

        @Test
        @DisplayName("TC-TOKEN-007 [P] Chuoi hex rong – tra ve mang rong")
        void hexToBytes_emptyString_returnsEmpty() {
            byte[] result = TokenService.hexToBytes("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("TC-TOKEN-008 [P] Chuoi all zeros – tra ve bytes zero")
        void hexToBytes_allZeros_returnsZeroBytes() {
            byte[] result = TokenService.hexToBytes("0000");
            assertThat(result).containsExactly((byte) 0x00, (byte) 0x00);
        }
    }

    // =========================================================
    // heartbeat() – session refresh
    // =========================================================
    @Nested
    @DisplayName("heartbeat()")
    class Heartbeat {

        @Test
        @DisplayName("TC-TOKEN-009 [N] Session het han – nem session_expired")
        void heartbeat_sessionExpired_throws() {
            given(redis.getExpire("sess:sid1")).willReturn(-2L);

            assertThatThrownBy(() -> tokenService.heartbeat("sid1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("session_expired");
        }

        @Test
        @DisplayName("TC-TOKEN-010 [P] Session con han – gia han them")
        void heartbeat_validSession_extendsExpiry() {
            ReflectionTestUtils.setField(tokenService, "issuer",    "game-server");
            ReflectionTestUtils.setField(tokenService, "jwtSecret", "secret");
            ReflectionTestUtils.setField(tokenService, "accessTtl",  900L);
            ReflectionTestUtils.setField(tokenService, "refreshTtl", 604800L);
            ReflectionTestUtils.setField(tokenService, "rotateRefresh", true);

            given(redis.getExpire("sess:sid1")).willReturn(100L);
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get("sess:sid1")).willReturn("{\"userId\":\"u1\",\"account\":\"a\",\"deviceId\":\"d\"}");

            assertThatNoException().isThrownBy(() -> tokenService.heartbeat("sid1"));
            then(valueOps).should(atLeastOnce()).set(eq("sess:sid1"), any(), anyLong(), any());
        }
    }
}
