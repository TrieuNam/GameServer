package com.SouthMillion.gameworld_service.service;

import com.SouthMillion.gameworld_service.dto.NearbyPlayersResponse;
import com.SouthMillion.gameworld_service.dto.PlayerPosition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("GameWorldService Tests")
class GameWorldServiceTest {

    @Mock private RedisTemplate<String, Object>   redisTemplate;
    @Mock private ObjectMapper                    objectMapper;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private SetOperations<String, Object>   setOps;

    @InjectMocks private GameWorldService gameWorldService;

    private static PlayerPosition pos(Long id, Integer mapId, Float x, Float y) {
        PlayerPosition p = new PlayerPosition();
        p.setPlayerId(id);
        p.setMapId(mapId);
        p.setX(x);
        p.setY(y);
        return p;
    }

    // =========================================================
    // updatePosition
    // =========================================================
    @Nested
    @DisplayName("updatePosition()")
    class UpdatePosition {

        @Test
        @DisplayName("TC-GW-001 [P] Cap nhat vi tri nguoi choi – luu vao Redis")
        void updatePosition_validPosition_savesToRedis() {
            PlayerPosition position = pos(1L, 101, 500.0f, 300.0f);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(redisTemplate.opsForSet()).willReturn(setOps);
            given(redisTemplate.expire(anyString(), anyLong(), any())).willReturn(true);

            PlayerPosition result = gameWorldService.updatePosition(position);

            assertThat(result).isEqualTo(position);
            assertThat(result.getTimestamp()).isGreaterThan(0);
            then(valueOps).should().set(contains("gameworld:position:1"), eq(position), anyLong(), any());
            then(setOps).should().add(contains("gameworld:map:101"), eq(1L));
        }
    }

    // =========================================================
    // getPosition
    // =========================================================
    @Nested
    @DisplayName("getPosition()")
    class GetPosition {

        @Test
        @DisplayName("TC-GW-002 [P] Lay vi tri nguoi choi – tra ve Optional co gia tri")
        void getPosition_found_returnsPosition() {
            PlayerPosition position = pos(1L, 101, 100.0f, 200.0f);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("gameworld:position:1")).willReturn(position);

            Optional<PlayerPosition> result = gameWorldService.getPosition(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getPlayerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC-GW-003 [N] Nguoi choi chua dang ky vi tri – tra ve Optional rong")
        void getPosition_notFound_returnsEmpty() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("gameworld:position:99")).willReturn(null);

            Optional<PlayerPosition> result = gameWorldService.getPosition(99L);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // getNearbyPlayers
    // =========================================================
    @Nested
    @DisplayName("getNearbyPlayers()")
    class GetNearbyPlayers {

        @Test
        @DisplayName("TC-GW-004 [N] Nguoi choi chua co vi tri – tra ve danh sach rong")
        void getNearbyPlayers_noPosition_returnsEmpty() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("gameworld:position:1")).willReturn(null);

            NearbyPlayersResponse result = gameWorldService.getNearbyPlayers(1L, 101, 100.0f);

            assertThat(result.getCount()).isEqualTo(0);
            assertThat(result.getPlayers()).isEmpty();
        }

        @Test
        @DisplayName("TC-GW-005 [P] Nguoi choi co vi tri – tra ve nguoi choi gan nhat")
        void getNearbyPlayers_withPosition_returnsNearby() {
            PlayerPosition requesterPos = pos(1L, 101, 0.0f, 0.0f);
            PlayerPosition nearbyPos    = pos(2L, 101, 10.0f, 10.0f); // distance ~14.1

            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("gameworld:position:1")).willReturn(requesterPos);
            given(valueOps.get("gameworld:position:2")).willReturn(nearbyPos);
            given(redisTemplate.opsForSet()).willReturn(setOps);
            given(setOps.members("gameworld:map:101")).willReturn(Set.of(2L));

            NearbyPlayersResponse result = gameWorldService.getNearbyPlayers(1L, 101, 100.0f);

            assertThat(result.getCount()).isEqualTo(1);
            assertThat(result.getPlayers().get(0).getPlayerId()).isEqualTo(2L);
        }
    }

    // =========================================================
    // removePlayer
    // =========================================================
    @Nested
    @DisplayName("removePlayer()")
    class RemovePlayer {

        @Test
        @DisplayName("TC-GW-006 [P] Xoa nguoi choi khoi the gioi – tra ve true")
        void removePlayer_exists_returnsTrue() {
            PlayerPosition position = pos(5L, 101, 100.0f, 200.0f);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("gameworld:position:5")).willReturn(position);
            given(redisTemplate.opsForSet()).willReturn(setOps);
            given(redisTemplate.delete("gameworld:position:5")).willReturn(true);

            boolean result = gameWorldService.removePlayer(5L);

            assertThat(result).isTrue();
            then(setOps).should().remove("gameworld:map:101", 5L);
        }

        @Test
        @DisplayName("TC-GW-007 [N] Nguoi choi khong co trong the gioi – tra ve false")
        void removePlayer_notExists_returnsFalse() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("gameworld:position:99")).willReturn(null);
            given(redisTemplate.delete("gameworld:position:99")).willReturn(false);

            boolean result = gameWorldService.removePlayer(99L);

            assertThat(result).isFalse();
        }
    }

    // =========================================================
    // validatePosition
    // =========================================================
    @Nested
    @DisplayName("validatePosition()")
    class ValidatePosition {

        @Test
        @DisplayName("TC-GW-008 [P] Vi tri hop le – tra ve true")
        void validatePosition_valid_returnsTrue() {
            PlayerPosition p = pos(1L, 101, 500.0f, 300.0f);
            assertThat(gameWorldService.validatePosition(p)).isTrue();
        }

        @Test
        @DisplayName("TC-GW-009 [N] Vi tri null – tra ve false")
        void validatePosition_null_returnsFalse() {
            assertThat(gameWorldService.validatePosition(null)).isFalse();
        }

        @Test
        @DisplayName("TC-GW-010 [N] Toa do vuot qua gioi han ban do – tra ve false")
        void validatePosition_outOfBounds_returnsFalse() {
            PlayerPosition p = pos(1L, 101, 15000.0f, 300.0f); // x > 10000
            assertThat(gameWorldService.validatePosition(p)).isFalse();
        }

        @Test
        @DisplayName("TC-GW-011 [N] Toa do am – tra ve false")
        void validatePosition_negative_returnsFalse() {
            PlayerPosition p = pos(1L, 101, -1.0f, 300.0f);
            assertThat(gameWorldService.validatePosition(p)).isFalse();
        }
    }
}
