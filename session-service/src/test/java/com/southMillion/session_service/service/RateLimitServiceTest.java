package com.SouthMillion.session_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

    @Mock private StringRedisTemplate redis;

    @InjectMocks private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "enabled",      true);
        ReflectionTestUtils.setField(rateLimitService, "windowSeconds", 60L);
        ReflectionTestUtils.setField(rateLimitService, "loginLimit",    10L);
        ReflectionTestUtils.setField(rateLimitService, "refreshLimit",  60L);
        ReflectionTestUtils.setField(rateLimitService, "genericLimit",  300L);
    }

    // =========================================================
    // checkLogin()
    // =========================================================
    @Nested
    @DisplayName("checkLogin()")
    class CheckLogin {

        @Test
        @DisplayName("TC-RL-001 [P] So luong request trong gioi han – khong nem ngoai le")
        void checkLogin_withinLimit_passes() {
            given(redis.execute(any(), any(), any())).willReturn(5L); // count=5 <= 10

            assertThatNoException().isThrownBy(
                    () -> rateLimitService.checkLogin("127.0.0.1", "user1"));
        }

        @Test
        @DisplayName("TC-RL-002 [N] Vuot qua gioi han – nem rate_limited")
        void checkLogin_exceedsLimit_throws() {
            given(redis.execute(any(), any(), any())).willReturn(11L); // count=11 > 10

            assertThatThrownBy(() -> rateLimitService.checkLogin("127.0.0.1", "user1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rate_limited");
        }

        @Test
        @DisplayName("TC-RL-003 [P] Rate limit bi tat – khong goi Redis")
        void checkLogin_disabled_skipsRedis() {
            ReflectionTestUtils.setField(rateLimitService, "enabled", false);

            assertThatNoException().isThrownBy(
                    () -> rateLimitService.checkLogin("127.0.0.1", "user1"));

            then(redis).should(never()).execute(any(), any(), any());
        }
    }

    // =========================================================
    // checkRefresh()
    // =========================================================
    @Nested
    @DisplayName("checkRefresh()")
    class CheckRefresh {

        @Test
        @DisplayName("TC-RL-004 [P] So luong refresh trong gioi han – khong nem ngoai le")
        void checkRefresh_withinLimit_passes() {
            given(redis.execute(any(), any(), any())).willReturn(30L); // count=30 <= 60

            assertThatNoException().isThrownBy(
                    () -> rateLimitService.checkRefresh("user1"));
        }

        @Test
        @DisplayName("TC-RL-005 [N] Vuot qua gioi han refresh – nem rate_limited")
        void checkRefresh_exceedsLimit_throws() {
            given(redis.execute(any(), any(), any())).willReturn(61L); // count=61 > 60

            assertThatThrownBy(() -> rateLimitService.checkRefresh("user1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rate_limited");
        }
    }

    // =========================================================
    // checkGeneric()
    // =========================================================
    @Nested
    @DisplayName("checkGeneric()")
    class CheckGeneric {

        @Test
        @DisplayName("TC-RL-006 [P] Yeu cau trong gioi han – khong nem ngoai le")
        void checkGeneric_withinLimit_passes() {
            given(redis.execute(any(), any(), any())).willReturn(100L);

            assertThatNoException().isThrownBy(
                    () -> rateLimitService.checkGeneric("10.0.0.1"));
        }

        @Test
        @DisplayName("TC-RL-007 [N] Vuot qua gioi han – nem rate_limited")
        void checkGeneric_exceedsLimit_throws() {
            given(redis.execute(any(), any(), any())).willReturn(301L);

            assertThatThrownBy(() -> rateLimitService.checkGeneric("10.0.0.1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rate_limited");
        }
    }
}
